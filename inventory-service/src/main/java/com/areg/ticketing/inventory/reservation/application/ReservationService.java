/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.application;

import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationResult;
import com.areg.ticketing.inventory.stock.application.StockNotFoundException;
import com.areg.ticketing.inventory.stock.application.StockRepository;
import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service: the operations of the ticket reservation flow. Every operation runs in one transaction,
 * so the stock and the reservation change together, and a concurrent change of the same stock is detected
 * by the version check (optimistic locking) when the transaction commits.
 */
@Transactional
public class ReservationService {

    private final StockRepository stocks;
    private final ReservationRepository reservations;
    private final Clock clock;
    private final Duration ttl;

    public ReservationService(StockRepository stocks, ReservationRepository reservations, Clock clock, Duration ttl) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.clock = clock;
        this.ttl = ttl;
    }

    /**
     * Holds tickets of an event for a user while they pay. Returns AlreadyHeld if the user still has a live
     * hold for this event, SoldOut if there are not enough tickets, and otherwise saves the new reservation
     * together with the reduced stock. An overdue hold of the same user that the cleanup has not reached yet is
     * expired first, so its tickets are freed and the one-hold-per-user rule never blocks a valid request.
     */
    public ReservationResult reserve(UUID userId, UUID eventId, int quantity) {
        final Instant now = clock.instant();
        final TicketStock stock = findStock(eventId);

        final Optional<Reservation> held = reservations.findHeld(userId, eventId);
        if (held.isPresent()) {
            if (!held.get().isExpiredAt(now)) {
                return new ReservationResult.AlreadyHeld(held.get().id());
            }
            expire(held.get(), stock, now);
        }

        final ReservationResult result = stock.reserve(userId, quantity, now, ttl);
        if (result instanceof ReservationResult.Reserved(Reservation reservation)) {
            reservations.add(reservation);
        }
        stocks.save(stock);
        return result;
    }

    /** Returns a reservation of the user. Reservations of other users look the same as unknown ones: not found. */
    @Transactional(readOnly = true)
    public Reservation get(UUID userId, UUID reservationId) {
        return findOwnedReservation(userId, reservationId);
    }

    /**
     * Marks a held reservation of the user as paid. Fails if it is unknown (or belongs to someone else), expired
     * or no longer HELD. The stock stays reduced.
     */
    public void confirm(UUID userId, UUID reservationId) {
        final Reservation reservation = findOwnedReservation(userId, reservationId);
        reservation.confirm(clock.instant());
        reservations.save(reservation);
    }

    /**
     * Cancels a held reservation of the user, for example after a failed payment. The reservation is released
     * first (this fails if it is not HELD), then its tickets go back to the stock, so tickets are never returned
     * twice. A reservation of another user is treated as not found.
     */
    public void release(UUID userId, UUID reservationId) {
        final Reservation reservation = findOwnedReservation(userId, reservationId);
        final TicketStock stock = findStock(reservation.eventId());

        reservation.release();
        stock.restore(reservation.quantity());

        reservations.save(reservation);
        stocks.save(stock);
    }

    /** Run periodically: expires every HELD reservation past its TTL, returns its tickets, and counts them. */
    public int expireStale() {
        final Instant now = clock.instant();
        int expired = 0;
        for (final Reservation reservation : reservations.findExpiredHeld(now)) {
            final TicketStock stock = findStock(reservation.eventId());

            expire(reservation, stock, now);
            stocks.save(stock);
            expired++;
        }
        return expired;
    }

    /** Expires the reservation and gives its tickets back to the (not yet saved) stock. */
    private void expire(Reservation reservation, TicketStock stock, Instant now) {
        reservation.expire(now);
        stock.restore(reservation.quantity());
        reservations.save(reservation);
    }

    private TicketStock findStock(UUID eventId) {
        return stocks.findByEventId(eventId).orElseThrow(() -> new StockNotFoundException(eventId));
    }

    /** Not found, and not "forbidden", when the reservation is someone else's, so ids cannot be probed. */
    private Reservation findOwnedReservation(UUID userId, UUID reservationId) {
        return reservations.findById(reservationId)
                .filter(reservation -> reservation.userId().equals(userId))
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}

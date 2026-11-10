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
     * together with the reduced stock.
     */
    public ReservationResult reserve(UUID userId, UUID eventId, int quantity) {
        final Instant now = clock.instant();
        final TicketStock stock = findStock(eventId);

        final var alreadyHeld = reservations.findHeld(userId, eventId).filter(held -> !held.isExpiredAt(now));
        if (alreadyHeld.isPresent()) {
            return new ReservationResult.AlreadyHeld(alreadyHeld.get().id());
        }

        final ReservationResult result = stock.reserve(userId, quantity, now, ttl);
        if (result instanceof ReservationResult.Reserved(Reservation reservation)) {
            stocks.save(stock);
            reservations.save(reservation);
        }
        return result;
    }

    /** Marks a held reservation as paid. Fails if it is unknown, expired or no longer HELD. The stock stays reduced. */
    public void confirm(UUID reservationId) {
        final Reservation reservation = findReservation(reservationId);
        reservation.confirm(clock.instant());
        reservations.save(reservation);
    }

    /**
     * Cancels a held reservation, for example after a failed payment. The reservation is released first
     * (this fails if it is not HELD), then its tickets go back to the stock, so tickets are never returned twice.
     */
    public void release(UUID reservationId) {
        final Reservation reservation = findReservation(reservationId);
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

            reservation.expire(now);
            stock.restore(reservation.quantity());

            reservations.save(reservation);
            stocks.save(stock);
            expired++;
        }
        return expired;
    }

    private TicketStock findStock(UUID eventId) {
        return stocks.findByEventId(eventId).orElseThrow(() -> new StockNotFoundException(eventId));
    }

    private Reservation findReservation(UUID reservationId) {
        return reservations.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    }
}

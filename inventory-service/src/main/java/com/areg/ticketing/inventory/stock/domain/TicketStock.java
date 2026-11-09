/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.domain;

import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Ticket stock of one event. Invariant: {@code 0 <= available <= total}, so tickets can never be oversold.
 */
public final class TicketStock {

    private final UUID eventId;
    private final int total;
    private int available;

    private TicketStock(UUID eventId, int total, int available) {
        this.eventId = eventId;
        this.total = total;
        this.available = available;
    }

    /** Creates the stock of a new event with all {@code total} tickets available. */
    public static TicketStock create(UUID eventId, int total) {
        Objects.requireNonNull(eventId, "eventId");
        if (total <= 0) {
            throw new IllegalArgumentException("total must be positive: " + total);
        }
        return new TicketStock(eventId, total, total);
    }

    /**
     * Holds {@code quantity} tickets for a user: if enough are available, decreases the stock and returns
     * Reserved with a new reservation; otherwise returns SoldOut and leaves the stock unchanged.
     */
    public ReservationResult reserve(UUID userId, int quantity, Instant now, Duration ttl) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (quantity > available) {
            return new ReservationResult.SoldOut(available);
        }
        final Reservation reservation = Reservation.hold(eventId, userId, quantity, now, ttl);
        available -= quantity;
        return new ReservationResult.Reserved(reservation);
    }

    /** Returns tickets of a released or expired reservation back to the available stock. */
    public void restore(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (available + quantity > total) {
            throw new IllegalStateException(
                    "Restoring %d tickets would exceed total %d (available %d)".formatted(quantity, total, available));
        }
        available += quantity;
    }

    public UUID eventId() {
        return eventId;
    }

    public int total() {
        return total;
    }

    public int available() {
        return available;
    }
}

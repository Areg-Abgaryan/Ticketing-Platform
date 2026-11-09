/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A temporary hold on tickets. Starts as {@link ReservationStatus#HELD} and can leave that state only once:
 * confirmed after payment, released on cancellation, or expired when its time-to-live runs out.
 */
public final class Reservation {

    private final UUID id;
    private final UUID eventId;
    private final UUID userId;
    private final int quantity;
    private final Instant createdAt;
    private final Instant expiresAt;
    private ReservationStatus status;

    private Reservation(UUID id, UUID eventId, UUID userId, int quantity, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.HELD;
    }

    /** Creates a new reservation in HELD status that expires {@code ttl} after {@code now}. */
    public static Reservation hold(UUID eventId, UUID userId, int quantity, Instant now, Duration ttl) {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(ttl, "ttl");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        return new Reservation(UUID.randomUUID(), eventId, userId, quantity, now, now.plus(ttl));
    }

    /** Marks the reservation as paid. Allowed only while it is HELD and not yet expired. */
    public void confirm(Instant now) {
        requireHeld();
        if (isExpiredAt(now)) {
            throw new IllegalReservationStateException("Cannot confirm expired reservation " + id);
        }
        status = ReservationStatus.CONFIRMED;
    }

    /** Cancels the reservation (e.g. payment failed). The caller must give the tickets back to the stock. */
    public void release() {
        requireHeld();
        status = ReservationStatus.RELEASED;
    }

    /** Marks an unpaid reservation as expired once its TTL has run out. The caller must give the tickets back. */
    public void expire(Instant now) {
        requireHeld();
        if (!isExpiredAt(now)) {
            throw new IllegalReservationStateException("Reservation " + id + " has not expired yet");
        }
        status = ReservationStatus.EXPIRED;
    }

    /** A reservation is expired from the moment {@code expiresAt} is reached. */
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Guard: only a HELD reservation can change status, so every final status stays final. */
    private void requireHeld() {
        if (status != ReservationStatus.HELD) {
            throw new IllegalReservationStateException(
                    "Reservation %s must be HELD but is %s".formatted(id, status));
        }
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID userId() {
        return userId;
    }

    public int quantity() {
        return quantity;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public ReservationStatus status() {
        return status;
    }
}

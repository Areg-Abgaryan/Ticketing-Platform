/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

/** What the API returns about a reservation, so the domain object is never exposed directly. */
record ReservationResponse(UUID id, UUID eventId, int quantity, ReservationStatus status, Instant expiresAt) {

    /** Copies the values the client needs from the domain object; the user id is left out on purpose. */
    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(reservation.id(), reservation.eventId(), reservation.quantity(),
                reservation.status(), reservation.expiresAt());
    }
}

/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.domain;

import java.util.UUID;

/**
 * Outcome of an attempt to reserve tickets. It is sealed, so a {@code switch} over it must handle
 * every case and the compiler flags a missing one.
 */
public sealed interface ReservationResult {

    /** The tickets were taken out of the stock and are held by the new {@code reservation}. */
    record Reserved(Reservation reservation) implements ReservationResult {
    }

    /** Not enough tickets: {@code available} is how many are left, and the stock is unchanged. */
    record SoldOut(int available) implements ReservationResult {
    }

    /** The user already holds a reservation for this event, so no new one is created. */
    record AlreadyHeld(UUID reservationId) implements ReservationResult {
    }
}

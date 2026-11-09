/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.domain;

/**
 * Lifecycle of a reservation. It starts as {@link #HELD} and moves to exactly one of the other
 * statuses, which are all final.
 */
public enum ReservationStatus {

    /** Tickets are held for the user while the payment is pending. */
    HELD,

    /** Paid: the tickets belong to the user and never return to the stock. */
    CONFIRMED,

    /** Cancelled before expiry, for example because the payment failed: tickets go back to the stock. */
    RELEASED,

    /** The time-to-live ran out without payment: tickets go back to the stock. */
    EXPIRED
}

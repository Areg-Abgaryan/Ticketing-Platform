/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.domain;

public class IllegalReservationStateException extends RuntimeException {

    public IllegalReservationStateException(String message) {
        super(message);
    }
}

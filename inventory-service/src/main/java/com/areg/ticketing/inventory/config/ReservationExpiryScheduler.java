/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.config;

import com.areg.ticketing.inventory.reservation.application.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically frees the tickets of reservations that were not paid in time. */
@Component
class ReservationExpiryScheduler {

    private final ReservationService reservations;

    ReservationExpiryScheduler(ReservationService reservations) {
        this.reservations = reservations;
    }

    /** Waits one interval after the previous run finished (and after startup) before sweeping again. */
    @Scheduled(fixedDelayString = "${ticketing.inventory.expiry-sweep-interval}",
            initialDelayString = "${ticketing.inventory.expiry-sweep-interval}")
    void sweep() {
        reservations.expireStale();
    }
}

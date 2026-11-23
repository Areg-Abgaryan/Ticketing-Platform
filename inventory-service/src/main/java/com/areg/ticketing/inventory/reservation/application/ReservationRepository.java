/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.application;

import com.areg.ticketing.inventory.reservation.domain.Reservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port: how the application layer loads and stores reservations. */
public interface ReservationRepository {

    Optional<Reservation> findById(UUID id);

    /**
     * The user's reservation for the event that is still in HELD status, if any. There is at most one (the
     * database enforces it); it may already be past its TTL if the cleanup has not reached it yet.
     */
    Optional<Reservation> findHeld(UUID userId, UUID eventId);

    /** All HELD reservations whose time-to-live has run out at {@code now}. */
    List<Reservation> findExpiredHeld(Instant now);

    /** Inserts a reservation known to be new, without first looking it up. */
    void add(Reservation reservation);

    /** Stores the new status of an existing reservation (inserts it if it does not exist yet). */
    void save(Reservation reservation);
}

/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.application;

import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<UUID, Reservation> reservations = new LinkedHashMap<>();

    @Override
    public Optional<Reservation> findById(UUID id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public Optional<Reservation> findHeld(UUID userId, UUID eventId) {
        return reservations.values().stream()
                .filter(r -> r.status() == ReservationStatus.HELD)
                .filter(r -> r.userId().equals(userId) && r.eventId().equals(eventId))
                .findFirst();
    }

    @Override
    public List<Reservation> findExpiredHeld(Instant now) {
        return reservations.values().stream()
                .filter(r -> r.status() == ReservationStatus.HELD && r.isExpiredAt(now))
                .toList();
    }

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.id(), reservation);
    }

    public int count() {
        return reservations.size();
    }
}

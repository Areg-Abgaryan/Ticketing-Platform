/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.out.persistence;

import com.areg.ticketing.inventory.reservation.application.ReservationRepository;
import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Stores reservations in Postgres through JPA. */
@Repository
class JpaReservationRepository implements ReservationRepository {

    private final ReservationJpaRepository jpa;

    JpaReservationRepository(ReservationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return jpa.findById(id).map(JpaReservationRepository::toDomain);
    }

    @Override
    public Optional<Reservation> findHeld(UUID userId, UUID eventId) {
        return jpa.findFirstByUserIdAndEventIdAndStatus(userId, eventId, ReservationStatus.HELD)
                .map(JpaReservationRepository::toDomain);
    }

    @Override
    public List<Reservation> findExpiredHeld(Instant now) {
        return jpa.findByStatusAndExpiresAtLessThanEqual(ReservationStatus.HELD, now).stream()
                .map(JpaReservationRepository::toDomain)
                .toList();
    }

    /** Inserts a new reservation, or updates the status of an existing one (the only field that changes). */
    @Override
    public void save(Reservation reservation) {
        final ReservationEntity entity = jpa.findById(reservation.id())
                .orElseGet(() -> new ReservationEntity(reservation.id(), reservation.eventId(), reservation.userId(),
                        reservation.quantity(), reservation.status(), reservation.createdAt(),
                        reservation.expiresAt()));
        entity.setStatus(reservation.status());
        jpa.save(entity);
    }

    private static Reservation toDomain(ReservationEntity entity) {
        return Reservation.rehydrate(entity.id(), entity.eventId(), entity.userId(), entity.quantity(),
                entity.createdAt(), entity.expiresAt(), entity.status());
    }
}

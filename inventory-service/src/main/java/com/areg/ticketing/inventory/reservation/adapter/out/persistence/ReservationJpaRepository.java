/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.out.persistence;

import com.areg.ticketing.inventory.reservation.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    /**
     * Spring Data builds the query from the method name:
     * {@code WHERE user_id = ? AND event_id = ? AND status = ? LIMIT 1}. Finds the user's reservation for an
     * event in the given status, used to check whether they already hold tickets.
     */
    Optional<ReservationEntity> findFirstByUserIdAndEventIdAndStatus(UUID userId, UUID eventId,
                                                                     ReservationStatus status);

    /**
     * Generated query: {@code WHERE status = ? AND expires_at <= ?}. Finds reservations in the given status
     * whose expiry time has been reached, used by the cleanup that frees unpaid holds.
     */
    List<ReservationEntity> findByStatusAndExpiresAtLessThanEqual(ReservationStatus status, Instant time);
}

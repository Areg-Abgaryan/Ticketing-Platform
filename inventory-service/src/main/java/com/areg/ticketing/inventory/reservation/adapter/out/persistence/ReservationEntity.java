/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.out.persistence;

import com.areg.ticketing.inventory.reservation.domain.ReservationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Database row of a reservation. Only {@code status} changes after the row is created. */
@Entity
@Table(name = "reservation")
class ReservationEntity {

    @Id
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;
    private Instant createdAt;
    private Instant expiresAt;
    @Version
    private Long version;

    protected ReservationEntity() {
    }

    ReservationEntity(UUID id, UUID eventId, UUID userId, int quantity, ReservationStatus status,
                      Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    UUID id() {
        return id;
    }

    UUID eventId() {
        return eventId;
    }

    UUID userId() {
        return userId;
    }

    int quantity() {
        return quantity;
    }

    ReservationStatus status() {
        return status;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    void setStatus(ReservationStatus status) {
        this.status = status;
    }
}

/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/** Database row of a ticket stock. {@code version} makes concurrent updates of the same row detectable. */
@Entity
@Table(name = "ticket_stock")
class TicketStockEntity {

    @Id
    private UUID eventId;
    private int total;
    private int available;
    @Version
    private Long version;

    protected TicketStockEntity() {
    }

    TicketStockEntity(UUID eventId, int total, int available) {
        this.eventId = eventId;
        this.total = total;
        this.available = available;
    }

    UUID eventId() {
        return eventId;
    }

    int total() {
        return total;
    }

    int available() {
        return available;
    }

    void setAvailable(int available) {
        this.available = available;
    }
}

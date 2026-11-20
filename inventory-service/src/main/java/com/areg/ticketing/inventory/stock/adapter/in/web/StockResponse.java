/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.in.web;

import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.util.UUID;

/** What the API returns about the stock of an event, so the domain object is never exposed directly. */
record StockResponse(UUID eventId, int total, int available) {

    /** Copies the values the client needs from the domain object. */
    static StockResponse from(TicketStock stock) {
        return new StockResponse(stock.eventId(), stock.total(), stock.available());
    }
}

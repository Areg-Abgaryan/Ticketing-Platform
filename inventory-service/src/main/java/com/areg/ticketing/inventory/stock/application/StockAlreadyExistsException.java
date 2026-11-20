/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.application;

import java.util.UUID;

/** Thrown when stock is created for an event that already has one. */
public class StockAlreadyExistsException extends RuntimeException {

    public StockAlreadyExistsException(UUID eventId) {
        super("Stock already exists for event " + eventId);
    }
}

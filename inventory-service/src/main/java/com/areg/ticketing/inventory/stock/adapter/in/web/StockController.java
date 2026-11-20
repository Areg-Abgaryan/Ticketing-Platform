/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.in.web;

import com.areg.ticketing.inventory.stock.application.StockService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for the stock of an event. Creating stock is temporary and open to everyone: the real
 * source of stock will be the catalog service, and this will then be restricted to admins.
 */
@RestController
class StockController {

    private final StockService stocks;

    StockController(StockService stocks) {
        this.stocks = stocks;
    }

    /**
     * Puts {@code total} tickets of an event on sale, all of them available. 201 with the new stock,
     * 409 if the event already has stock, 400 if the total is not positive.
     */
    @PutMapping("/events/{eventId}/stock")
    ResponseEntity<StockResponse> create(@PathVariable UUID eventId, @Valid @RequestBody CreateStockRequest request) {
        return ResponseEntity.status(201).body(StockResponse.from(stocks.create(eventId, request.total())));
    }

    /** Shows how many tickets of an event exist and how many are still free to reserve. 404 if unknown. */
    @GetMapping("/events/{eventId}/availability")
    StockResponse availability(@PathVariable UUID eventId) {
        return StockResponse.from(stocks.get(eventId));
    }
}

/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import com.areg.ticketing.inventory.reservation.application.ReservationService;
import com.areg.ticketing.inventory.reservation.domain.ReservationResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** REST entry point of the reservation flow. Translates HTTP to service calls and results back to HTTP. */
@RestController
class ReservationController {

    private final ReservationService reservations;
    private final CurrentUser currentUser;

    ReservationController(ReservationService reservations, CurrentUser currentUser) {
        this.reservations = reservations;
        this.currentUser = currentUser;
    }

    /**
     * Holds tickets of an event for the calling user while they pay.
     * <ul>
     *   <li>201 with the reservation and its {@code Location} when the tickets are held</li>
     *   <li>409 "Sold out" with the number of tickets still {@code available}</li>
     *   <li>409 "Already reserved" with the {@code reservationId} the user already holds for this event</li>
     *   <li>404 if the event has no stock, 400 if the quantity is not positive</li>
     * </ul>
     */
    @PostMapping("/events/{eventId}/reservations")
    ResponseEntity<?> reserve(@PathVariable UUID eventId, @Valid @RequestBody ReserveRequest request) {
        final ReservationResult result = reservations.reserve(currentUser.id(), eventId, request.quantity());
        return switch (result) {
            case ReservationResult.Reserved reserved -> ResponseEntity
                    .created(URI.create("/reservations/" + reserved.reservation().id()))
                    .body(ReservationResponse.from(reserved.reservation()));
            case ReservationResult.SoldOut soldOut -> {
                final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Not enough tickets left");
                problem.setTitle("Sold out");
                problem.setProperty("available", soldOut.available());
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }
            case ReservationResult.AlreadyHeld alreadyHeld -> {
                final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "You already hold tickets for this event");
                problem.setTitle("Already reserved");
                problem.setProperty("reservationId", alreadyHeld.reservationId());
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }
        };
    }

    /**
     * Marks the reservation as paid; its tickets stay sold. 204 on success, 404 if unknown,
     * 409 if it has expired or is no longer HELD.
     */
    @PostMapping("/reservations/{id}/confirm")
    ResponseEntity<Void> confirm(@PathVariable UUID id) {
        reservations.confirm(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancels the reservation (e.g. the user gave up or payment failed) and puts its tickets back on sale.
     * 204 on success, 404 if unknown, 409 if it is no longer HELD, so tickets are never returned twice.
     */
    @PostMapping("/reservations/{id}/release")
    ResponseEntity<Void> release(@PathVariable UUID id) {
        reservations.release(id);
        return ResponseEntity.noContent().build();
    }
}

/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.web;

import com.areg.ticketing.inventory.reservation.application.ReservationNotFoundException;
import com.areg.ticketing.inventory.reservation.domain.IllegalReservationStateException;
import com.areg.ticketing.inventory.stock.application.StockAlreadyExistsException;
import com.areg.ticketing.inventory.stock.application.StockNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 7807 problem responses. Malformed or invalid requests (400) are already handled
 * by the parent class, here we add the business errors.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** 404: the requested event stock or reservation does not exist. */
    @ExceptionHandler({StockNotFoundException.class, ReservationNotFoundException.class})
    ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * 409: the request is valid but clashes with the current state, e.g. confirming a reservation that has
     * already expired, or creating stock for an event that already has it.
     */
    @ExceptionHandler({IllegalReservationStateException.class, StockAlreadyExistsException.class})
    ProblemDetail conflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}

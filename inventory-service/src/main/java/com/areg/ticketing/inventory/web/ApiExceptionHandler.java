/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.web;

import com.areg.ticketing.inventory.reservation.application.ReservationNotFoundException;
import com.areg.ticketing.inventory.reservation.domain.IllegalReservationStateException;
import com.areg.ticketing.inventory.stock.application.StockAlreadyExistsException;
import com.areg.ticketing.inventory.stock.application.StockNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 7807 problem responses. Malformed or invalid requests (400) are already handled
 * by the parent class, here we add the business errors. The generic handlers return fixed texts and never the
 * exception message, because those can come from the framework or the database and are not meant for clients.
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

    /**
     * 409 with {@code Retry-After}: the request lost a race with a concurrent one (version check or unique
     * constraint), nothing was changed, and the client may simply send it again.
     */
    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    ResponseEntity<ProblemDetail> concurrentConflict() {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request conflicted with a concurrent change, please try again");
        problem.setTitle("Concurrent update");
        return ResponseEntity.status(HttpStatus.CONFLICT).header(HttpHeaders.RETRY_AFTER, "1").body(problem);
    }

    /** 400: a value was rejected by the domain rules. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail illegalArgument() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request contains an invalid value");
    }

    /** 409: the operation is not possible in the current state of the stock or reservation. */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail illegalState() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request is not possible in the current state");
    }
}

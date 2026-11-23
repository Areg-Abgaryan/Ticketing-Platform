/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** How exceptions that are not business errors are turned into HTTP responses. */
class ApiExceptionHandlerTest {

    @RestController
    static class FailingController {

        @GetMapping("/version-conflict")
        void versionConflict() {
            throw new ObjectOptimisticLockingFailureException("TicketStockEntity", "secret-id");
        }

        @GetMapping("/constraint")
        void constraint() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint secret_index");
        }

        @GetMapping("/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("internal detail");
        }

        @GetMapping("/illegal-state")
        void illegalState() {
            throw new IllegalStateException("internal detail");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void versionConflictReturns409WithRetryAfter() throws Exception {
        mvc.perform(get("/version-conflict"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.title").value("Concurrent update"))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void constraintViolationReturns409WithoutLeakingTheDatabaseMessage() throws Exception {
        mvc.perform(get("/constraint"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void illegalArgumentReturns400WithoutLeakingTheMessage() throws Exception {
        mvc.perform(get("/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("internal"))));
    }

    @Test
    void illegalStateReturns409WithoutLeakingTheMessage() throws Exception {
        mvc.perform(get("/illegal-state"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("internal"))));
    }
}

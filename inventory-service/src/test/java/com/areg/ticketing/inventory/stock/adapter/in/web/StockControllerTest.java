/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areg.ticketing.inventory.stock.application.InMemoryStockRepository;
import com.areg.ticketing.inventory.stock.application.StockService;
import com.areg.ticketing.inventory.web.ApiExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP-level behavior of the stock endpoints, without starting Spring or a database. */
class StockControllerTest {

    private final UUID eventId = UUID.randomUUID();
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new StockController(new StockService(new InMemoryStockRepository())))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private ResultActions createStock(String body) throws Exception {
        return mvc.perform(put("/events/{id}/stock", eventId).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void createThenAvailabilityShowsAllTickets() throws Exception {
        createStock("{\"total\":100}").andExpect(status().isCreated());

        mvc.perform(get("/events/{id}/availability", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.available").value(100));
    }

    @Test
    void creatingStockTwiceReturns409() throws Exception {
        createStock("{\"total\":100}");

        createStock("{\"total\":50}").andExpect(status().isConflict());
    }

    @Test
    void invalidTotalReturns400() throws Exception {
        createStock("{\"total\":0}").andExpect(status().isBadRequest());
    }

    @Test
    void totalAboveTheLimitReturns400() throws Exception {
        createStock("{\"total\":1000001}").andExpect(status().isBadRequest());
    }

    @Test
    void availabilityOfUnknownEventReturns404() throws Exception {
        mvc.perform(get("/events/{id}/availability", eventId)).andExpect(status().isNotFound());
    }
}

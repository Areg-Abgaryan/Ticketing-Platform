/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.areg.ticketing.inventory.reservation.application.InMemoryReservationRepository;
import com.areg.ticketing.inventory.reservation.application.ReservationService;
import com.areg.ticketing.inventory.stock.application.InMemoryStockRepository;
import com.areg.ticketing.inventory.stock.domain.TicketStock;
import com.areg.ticketing.inventory.web.ApiExceptionHandler;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP-level behavior of the reservation endpoints, without starting Spring or a database. */
class ReservationControllerTest {

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final InMemoryStockRepository stocks = new InMemoryStockRepository();
    private final ReservationService service = new ReservationService(
            stocks, new InMemoryReservationRepository(), Clock.systemUTC(), Duration.ofMinutes(10));
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ReservationController(service, () -> userId))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private ResultActions reserve(UUID event, String body) throws Exception {
        return mvc.perform(post("/events/{id}/reservations", event)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String reserveAndGetId(int quantity) throws Exception {
        final String json = reserve(eventId, "{\"quantity\":" + quantity + "}")
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void reserveReturns201WithLocation() throws Exception {
        stocks.save(TicketStock.create(eventId, 5));

        reserve(eventId, "{\"quantity\":2}")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void soldOutReturns409WithAvailableCount() throws Exception {
        stocks.save(TicketStock.create(eventId, 1));

        reserve(eventId, "{\"quantity\":2}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.available").value(1));
    }

    @Test
    void secondReserveByTheSameUserReturns409WithExistingReservationId() throws Exception {
        stocks.save(TicketStock.create(eventId, 5));
        final String id = reserveAndGetId(1);

        reserve(eventId, "{\"quantity\":1}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reservationId").value(id));
    }

    @Test
    void unknownEventReturns404() throws Exception {
        reserve(UUID.randomUUID(), "{\"quantity\":1}").andExpect(status().isNotFound());
    }

    @Test
    void invalidQuantityReturns400() throws Exception {
        stocks.save(TicketStock.create(eventId, 5));

        reserve(eventId, "{\"quantity\":0}").andExpect(status().isBadRequest());
    }

    @Test
    void confirmReturns204AndSecondConfirmReturns409() throws Exception {
        stocks.save(TicketStock.create(eventId, 5));
        final String id = reserveAndGetId(1);

        mvc.perform(post("/reservations/{id}/confirm", id)).andExpect(status().isNoContent());
        mvc.perform(post("/reservations/{id}/confirm", id)).andExpect(status().isConflict());
    }

    @Test
    void releaseReturns204() throws Exception {
        stocks.save(TicketStock.create(eventId, 5));
        final String id = reserveAndGetId(1);

        mvc.perform(post("/reservations/{id}/release", id)).andExpect(status().isNoContent());
    }

    @Test
    void confirmUnknownReservationReturns404() throws Exception {
        mvc.perform(post("/reservations/{id}/confirm", UUID.randomUUID())).andExpect(status().isNotFound());
    }
}

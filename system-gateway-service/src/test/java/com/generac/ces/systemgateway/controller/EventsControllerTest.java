package com.generac.ces.systemgateway.controller;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.generac.ces.system.control.subcontrol.Subcontrol;
import com.generac.ces.systemgateway.controller.system.EventsController;
import com.generac.ces.systemgateway.model.EventsListResponse;
import com.generac.ces.systemgateway.model.common.Actor;
import com.generac.ces.systemgateway.model.common.EventStatus;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.service.odin.OdinService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = EventsController.class)
@AutoConfigureMockMvc
public class EventsControllerTest {

    @Autowired MockMvc mvc;

    @Autowired ObjectMapper objectMapper;

    @MockBean private OdinService odinService;

    // =======================================================================================================
    //   GET EVENTS LIST
    // =======================================================================================================
    @Test
    public void testGetEventsList_happyCaseOk() throws Exception {
        // Arrange
        UUID systemId = UUID.fromString("fae7d4a7-a74c-418b-a61a-433d92747358");
        String actorIds = "PWRFLEET";
        String subControlTypes = "SYSTEM_MODE";
        String start = DateTime.now().toString();
        String end = DateTime.now().toString();

        String messageId = "9069ddb8-312d-4da7-927c-7e36a81fe6e2";
        OffsetDateTime requestTimestamp =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606554), ZoneOffset.UTC);
        OffsetDateTime startTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606554), ZoneOffset.UTC);
        OffsetDateTime endTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606558), ZoneOffset.UTC);
        OffsetDateTime scheduleStartTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606554), ZoneOffset.UTC);
        OffsetDateTime scheduleEndTime =
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(1696606554), ZoneOffset.UTC);

        EventsListResponse mockResponse =
                new EventsListResponse(
                        List.of(
                                EventsListResponse.Details.builder()
                                        .eventActual(
                                                new EventsListResponse.EventActual(
                                                        startTime, endTime))
                                        .eventSchedule(
                                                new EventsListResponse.EventSchedule(
                                                        scheduleStartTime, scheduleEndTime))
                                        .status(EventStatus.COMPLETE)
                                        .event(
                                                new EventsListResponse.Event(
                                                        EventsListResponse.Setting.builder()
                                                                .requests(
                                                                        List.of(
                                                                                EventsListResponse
                                                                                        .DeviceRequest
                                                                                        .builder()
                                                                                        .systemModeRequest(
                                                                                                new EventsListResponse
                                                                                                        .SystemModeRequest(
                                                                                                        true,
                                                                                                        SystemMode
                                                                                                                .CLEAN_BACKUP,
                                                                                                        false))
                                                                                        .build()))
                                                                .build(),
                                                        null))
                                        .messageId(messageId)
                                        .requestTimestamp(requestTimestamp)
                                        .build()));

        when(odinService.getEventsList(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(mockResponse));

        // Action
        MvcResult mvcResult =
                mvc.perform(
                                get("/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/events")
                                        .queryParam("actorIds", actorIds)
                                        .queryParam("subControlTypes", subControlTypes)
                                        .queryParam("start", start)
                                        .queryParam("end", end))
                        .andExpect(status().isOk())
                        .andReturn();

        // Assert
        verify(odinService, times(1))
                .getEventsList(
                        systemId.toString(),
                        Collections.singletonList(Actor.PWRFLEET),
                        Collections.singletonList(
                                Subcontrol.SubControlType.valueOf(subControlTypes)),
                        start,
                        end);

        String expected = String.valueOf(Mono.just(mockResponse).block());
        String actual = mvcResult.getAsyncResult().toString();

        assertEquals(expected, actual);
    }

    @Test
    public void testGetEventsList_invalidSystemIdThrowsBadRequest() throws Exception {
        // Action & Assert
        mvc.perform(get("/v1/systems/12345/events")).andExpect(status().isBadRequest());
        verify(odinService, times(0)).getEventsList(any(), any(), any(), any(), any());
    }

    @Test
    public void testGetEventsList_missingStartTimeThrowsBadRequest() throws Exception {
        // Arrange
        String actorIds = "[\"pwrfleet\"]";
        String subControlTypes = "SYSTEM_MODE";
        String end = DateTime.now().toString();

        // Action & Assert
        mvc.perform(
                        get("/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/events")
                                .queryParam("actorIds", actorIds)
                                .queryParam("subControlTypes", subControlTypes)
                                .queryParam("end", end))
                .andExpect(status().isBadRequest());

        verify(odinService, times(0)).getEventsList(any(), any(), any(), any(), any());
    }

    @Test
    public void testGetEventsList_invalidStartTimeThrowsBadRequest() throws Exception {
        // Arrange
        String actorIds = "PWRFLEET";
        String subControlTypes = "SYSTEM_MODE";
        String start = "invalid-time";
        String end = DateTime.now().toString();

        // Action
        MvcResult mvcResult =
                mvc.perform(
                                get("/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/events")
                                        .queryParam("actorIds", actorIds)
                                        .queryParam("subControlTypes", subControlTypes)
                                        .queryParam("start", start)
                                        .queryParam("end", end))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        // Assert
        verify(odinService, times(0)).getEventsList(any(), any(), any(), any(), any());

        String expected = "Invalid start value";
        String actual = Objects.requireNonNull(mvcResult.getResolvedException()).getMessage();
        assertTrue(actual.contains(expected));
    }

    @Test
    public void testGetEventsList_invalidEndTimeThrowsBadRequest() throws Exception {
        // Arrange
        String actorIds = "PWRFLEET";
        String subControlTypes = "SYSTEM_MODE";
        String start = DateTime.now().toString();
        String end = "invalid-time";

        // Action
        MvcResult mvcResult =
                mvc.perform(
                                get("/v1/systems/fae7d4a7-a74c-418b-a61a-433d92747358/events")
                                        .queryParam("actorIds", actorIds)
                                        .queryParam("subControlTypes", subControlTypes)
                                        .queryParam("start", start)
                                        .queryParam("end", end))
                        .andExpect(status().isBadRequest())
                        .andReturn();

        // Assert
        verify(odinService, times(0)).getEventsList(any(), any(), any(), any(), any());

        String expected = "Invalid end value";
        String actual = Objects.requireNonNull(mvcResult.getResolvedException()).getMessage();
        assertTrue(actual.contains(expected));
    }
}

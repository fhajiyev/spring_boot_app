package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.generac.ces.systemgateway.model.common.Actor;
import com.generac.ces.systemgateway.model.common.CancellationType;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.common.EventStatus;
import com.generac.ces.systemgateway.model.common.SubControlType;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventsListResponse(List<Details> events) {
    /**
     * Events List Details
     *
     * @param messageId - The ID of the message.
     * @param requestTimestamp - The timestamp when the request was received.
     * @param actor - The actorId and priority of the event.
     * @param event - Contains event details dependent upon sub-control type for the event.
     * @param status - The current status of the event.
     * @param cancellationType - If the event is cancelled, this field indicates the type of
     *     cancellation.
     * @param eventSchedule - Comprised of the scheduled start and end time of an event.
     * @param eventActual - Comprised of the actual start and end time of an event.
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Details(
            String messageId,
            @JsonSerialize(using = OffsetDateTimeSerializer.class) OffsetDateTime requestTimestamp,
            Actor actor,
            Event event,
            EventStatus status,
            CancellationType cancellationType,
            EventSchedule eventSchedule,
            EventActual eventActual) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Event(Setting setting, Setting reversionSetting) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Setting(SubControlType subControl, List<DeviceRequest> requests) {}

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeviceRequest {
        @JsonUnwrapped BatterySettingRequest batterySettingRequest;
        @JsonUnwrapped SystemModeRequest systemModeRequest;
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BatterySettingRequest(
            List<SettingNameValue> settings,
            DeviceState state,
            String rcpn,
            Boolean cancelOnExternalChange) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SettingNameValue(DeviceSetting name, Double value) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SystemModeRequest(
            Boolean touEnabled, SystemMode systemMode, Boolean cancelOnExternalChange) {}

    public record EventSchedule(
            @JsonSerialize(using = OffsetDateTimeSerializer.class) OffsetDateTime startTime,
            @JsonSerialize(using = OffsetDateTimeSerializer.class) OffsetDateTime endTime) {}

    public record EventActual(
            @JsonSerialize(using = OffsetDateTimeSerializer.class) OffsetDateTime startTime,
            @JsonSerialize(using = OffsetDateTimeSerializer.class) OffsetDateTime endTime) {}
}

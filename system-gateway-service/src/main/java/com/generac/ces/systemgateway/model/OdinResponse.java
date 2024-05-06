package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.generac.ces.system.control.message.NotificationOuterClass;
import com.generac.ces.systemgateway.model.enums.NotificationType;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OdinResponse implements Serializable {
    private static final long serialVersionUID = 2405172041950251807L;

    private String id;
    private String code;
    private String errorMessage;
    private EventDetails eventDetails;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = EventSettingsChanged.class, name = "EVENT_SETTINGS_CHANGED"),
        @JsonSubTypes.Type(value = EventCanceled.class, name = "EVENT_CANCELED"),
        @JsonSubTypes.Type(value = EventError.class, name = "EVENT_ERROR")
    })
    public static class EventDetails {}

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EventCanceled extends OdinResponse.EventDetails {
        private NotificationType eventType = NotificationType.EVENT_CANCELED;
        private OffsetDateTime eventCanceledTime;
        private String cancelationType; // typo as per proto
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EventError extends OdinResponse.EventDetails {
        private NotificationType eventType = NotificationType.EVENT_ERROR;
        private NotificationOuterClass.EventErrorCode eventErrorType;
        private String deviceRcpn;
        private String eventErrorMessage;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class EventSettingsChanged extends OdinResponse.EventDetails {
        private NotificationType eventType = NotificationType.EVENT_SETTINGS_CHANGED;
        private OffsetDateTime eventSettingsChangedTime;
        private BatterySettingResponseDto battery;
        private InverterSettingResponseDto inverter;
        private OdinSystemModeSettingResponse systemMode;
        private PvlSettingResponseDto pvLink;
    }
}

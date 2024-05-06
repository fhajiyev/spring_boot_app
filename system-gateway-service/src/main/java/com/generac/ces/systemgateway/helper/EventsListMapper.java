package com.generac.ces.systemgateway.helper;

import com.generac.ces.system.control.message.EventOuterClass;
import com.generac.ces.system.control.message.List;
import com.generac.ces.system.control.subcontrol.BatterySettingControlOuterClass;
import com.generac.ces.system.control.subcontrol.SystemModeControlOuterClass;
import com.generac.ces.systemgateway.model.EventsListResponse;
import com.generac.ces.systemgateway.model.common.Actor;
import com.generac.ces.systemgateway.model.common.CancellationType;
import com.generac.ces.systemgateway.model.common.DeviceState;
import com.generac.ces.systemgateway.model.common.EventStatus;
import com.generac.ces.systemgateway.model.common.SubControlType;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.enums.DeviceSetting;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface EventsListMapper {

    EventsListMapper INSTANCE = Mappers.getMapper(EventsListMapper.class);

    @Mapping(target = "events", expression = "java(constructEventsListResponse(listResponse))")
    EventsListResponse convert(List.ListResponse listResponse);

    @Mapping(
            target = "event",
            expression =
                    "java(event.hasBattery() ? constructBatteryEvent(event.getBattery()) :"
                            + " event.hasSysmode() ? constructSystemModeEvent(event.getSysmode()) :"
                            + " null)")
    @Mapping(
            target = "requestTimestamp",
            expression = "java(timestampToOffsetDateTime(event.getRequestTimestamp()))")
    @Mapping(
            target = "eventSchedule.startTime",
            expression = "java(timestampToOffsetDateTime(eventSchedule.getScheduleStartTime()))")
    @Mapping(
            target = "eventSchedule.endTime",
            expression = "java(timestampToOffsetDateTime(eventSchedule.getScheduleEndTime()))")
    @Mapping(
            target = "eventActual.startTime",
            expression = "java(timestampToOffsetDateTime(eventActual.getStartTime()))")
    @Mapping(
            target = "eventActual.endTime",
            expression = "java(timestampToOffsetDateTime(eventActual.getEndTime()))")
    @Mapping(
            target = "cancellationType",
            expression = "java(getCancellationType(event.getCancelationTypeValue()))")
    @Mapping(target = "status", expression = "java(getEventStatus(event.getStatusValue()))")
    EventsListResponse.Details convertEventDetails(EventOuterClass.Event event, Actor actor);

    default java.util.List<EventsListResponse.Details> constructEventsListResponse(
            List.ListResponse listResponse) {
        java.util.List<EventsListResponse.Details> result = new ArrayList<>();
        listResponse
                .getEvents()
                .getActorEventsList()
                .forEach(
                        r -> {
                            Actor actor = Actor.fromActorId(r.getActor().getActorId());
                            r.getEventsList()
                                    .forEach(s -> result.add(convertEventDetails(s, actor)));
                        });
        return result;
    }

    default EventsListResponse.Event constructBatteryEvent(
            BatterySettingControlOuterClass.BatterySettingControl batterySettingControlEvent) {
        return new EventsListResponse.Event(
                EventsListResponse.Setting.builder()
                        .subControl(SubControlType.BATTERY)
                        .requests(toBatterySetting(batterySettingControlEvent.getSettingList()))
                        .build(),
                EventsListResponse.Setting.builder()
                        .subControl(
                                batterySettingControlEvent.getReversionSettingList().isEmpty()
                                        ? null
                                        : SubControlType.BATTERY)
                        .requests(
                                batterySettingControlEvent.getReversionSettingList().isEmpty()
                                        ? null
                                        : toBatterySetting(
                                                batterySettingControlEvent
                                                        .getReversionSettingList()))
                        .build());
    }

    default java.util.List<EventsListResponse.DeviceRequest> toBatterySetting(
            java.util.List<BatterySettingControlOuterClass.BatterySetting>
                    batterySettingControlList) {
        java.util.List<EventsListResponse.DeviceRequest> result = new ArrayList<>();
        batterySettingControlList.forEach(
                r -> {
                    java.util.List<EventsListResponse.SettingNameValue> setting = new ArrayList<>();
                    addSettingIfNotNull(
                            setting, DeviceSetting.SOC_MAX, r.hasSocMax() ? r.getSocMax() : null);
                    addSettingIfNotNull(
                            setting, DeviceSetting.SOC_MIN, r.hasSocMin() ? r.getSocMin() : null);
                    addSettingIfNotNull(
                            setting,
                            DeviceSetting.SOC_RSV_MAX,
                            r.hasSocRsvMax() ? r.getSocRsvMax() : null);
                    addSettingIfNotNull(
                            setting,
                            DeviceSetting.SOC_RSV_MIN,
                            r.hasSocRsvMin() ? r.getSocRsvMin() : null);
                    addSettingIfNotNull(
                            setting,
                            DeviceSetting.A_CHA_MAX,
                            r.hasAChaMax() ? r.getAChaMax() : null);
                    addSettingIfNotNull(
                            setting,
                            DeviceSetting.A_DISCHA_MAX,
                            r.hasADisChaMax() ? r.getADisChaMax() : null);

                    result.add(
                            new EventsListResponse.DeviceRequest(
                                    EventsListResponse.BatterySettingRequest.builder()
                                            .settings(setting.isEmpty() ? null : setting)
                                            .state(
                                                    r.hasState()
                                                            ? DeviceState.fromValue(
                                                                    r.getStateValue())
                                                            : null)
                                            .rcpn(r.getRcpn())
                                            .cancelOnExternalChange(r.getCancelOnExternalChange())
                                            .build(),
                                    null));
                });
        return result;
    }

    private static void addSettingIfNotNull(
            java.util.List<EventsListResponse.SettingNameValue> settingNameValues,
            DeviceSetting setting,
            Double value) {
        if (value != null) {
            settingNameValues.add(
                    EventsListResponse.SettingNameValue.builder()
                            .name(setting)
                            .value(value)
                            .build());
        }
    }

    default EventsListResponse.Event constructSystemModeEvent(
            SystemModeControlOuterClass.SystemModeControl systemModeControlEvent) {
        return new EventsListResponse.Event(
                EventsListResponse.Setting.builder()
                        .subControl(SubControlType.SYSTEM_MODE)
                        .requests(
                                java.util.List.of(
                                        toSystemModeSetting(systemModeControlEvent.getSetting())))
                        .build(),
                EventsListResponse.Setting.builder()
                        .subControl(
                                !systemModeControlEvent.hasReversionSetting()
                                        ? null
                                        : SubControlType.SYSTEM_MODE)
                        .requests(
                                !systemModeControlEvent.hasReversionSetting()
                                        ? null
                                        : java.util.List.of(
                                                toSystemModeReversionSetting(
                                                        systemModeControlEvent
                                                                .getReversionSetting())))
                        .build());
    }

    default EventsListResponse.DeviceRequest toSystemModeSetting(
            SystemModeControlOuterClass.SystemModeSetting systemModeSetting) {
        return new EventsListResponse.DeviceRequest(
                null,
                EventsListResponse.SystemModeRequest.builder()
                        .systemMode(
                                systemModeSetting.hasSystemMode()
                                        ? SystemMode.fromMode(systemModeSetting.getSystemMode())
                                        : null)
                        .touEnabled(
                                systemModeSetting.hasTouEnabled()
                                        ? systemModeSetting.getTouEnabled()
                                        : null)
                        .cancelOnExternalChange(systemModeSetting.getCancelOnExternalChange())
                        .build());
    }

    default EventsListResponse.DeviceRequest toSystemModeReversionSetting(
            SystemModeControlOuterClass.SystemModeReversionSetting systemModeReversionSetting) {
        return new EventsListResponse.DeviceRequest(
                null,
                EventsListResponse.SystemModeRequest.builder()
                        .systemMode(
                                systemModeReversionSetting.hasSystemMode()
                                        ? SystemMode.fromMode(
                                                systemModeReversionSetting.getSystemMode())
                                        : null)
                        .touEnabled(
                                systemModeReversionSetting.hasTouEnabled()
                                        ? systemModeReversionSetting.getTouEnabled()
                                        : null)
                        .build());
    }

    default OffsetDateTime timestampToOffsetDateTime(Timestamp timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    default CancellationType getCancellationType(Integer value) {
        return CancellationType.fromValue(value);
    }

    default EventStatus getEventStatus(Integer value) {
        return EventStatus.fromValue(value);
    }
}

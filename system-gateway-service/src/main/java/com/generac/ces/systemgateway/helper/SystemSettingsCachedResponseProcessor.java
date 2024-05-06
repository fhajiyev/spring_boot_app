package com.generac.ces.systemgateway.helper;

import com.generac.ces.systemgateway.model.ParameterTimestampValueMap;
import com.generac.ces.systemgateway.model.common.SystemMode;
import com.generac.ces.systemgateway.model.system.SystemModeGetResponse;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SystemSettingsCachedResponseProcessor {

    public SystemSettingsCachedResponseProcessor() {}

    public static SystemModeGetResponse fromCachedData(
            UUID systemId,
            ParameterTimestampValueMap<SystemMode> cachedData,
            List<SystemMode> activeModes) {
        OffsetDateTime updatedTimestampUtc =
                cachedData.getEntryMap().values().stream()
                        .map(ParameterTimestampValueMap.ParameterTimestampValue::getTimestamp)
                        .max(OffsetDateTime::compareTo)
                        .orElse(OffsetDateTime.now());

        SystemMode mode =
                (SystemMode)
                        cachedData.getEntryMap().values().stream()
                                .max(
                                        Comparator.comparing(
                                                ParameterTimestampValueMap.ParameterTimestampValue
                                                        ::getTimestamp))
                                .map(ParameterTimestampValueMap.ParameterTimestampValue::getValue)
                                .orElse(null);

        return com.generac.ces.systemgateway.model.system.SystemModeGetResponse.builder()
                .systemId(systemId)
                .mode(mode)
                .activeModes(activeModes)
                .updatedTimestampUtc(updatedTimestampUtc)
                .build();
    }
}

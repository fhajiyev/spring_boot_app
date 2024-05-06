package com.generac.ces.systemgateway.helper;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.mapstruct.Named;

public class Formatter {

    @Named("timeStampToLong")
    static Long timeStampToLong(Timestamp time) {
        if (time == null) {
            return null;
        }
        ZonedDateTime utcDateTime = time.toLocalDateTime().atZone(ZoneId.of("UTC"));
        return utcDateTime.toEpochSecond();
    }
}

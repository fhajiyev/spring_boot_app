package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeSerializer extends JsonSerializer<OffsetDateTime> {
    @Override
    public void serialize(
            OffsetDateTime offsetDateTime,
            JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider)
            throws IOException {
        String formattedDateTime = offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        jsonGenerator.writeString(formattedDateTime);
    }
}

package com.generac.ces.systemgateway.model;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class OffsetDateTimeSerializerTest {
    private OffsetDateTimeSerializer serializer;

    @Mock private JsonGenerator jsonGenerator;

    @Mock private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        openMocks(this);
        serializer = new OffsetDateTimeSerializer();
    }

    @Test
    void testSerialize() throws IOException {
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        serializer.serialize(offsetDateTime, jsonGenerator, serializerProvider);

        String formattedDateTime = offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        verify(jsonGenerator).writeString(formattedDateTime);
    }
}

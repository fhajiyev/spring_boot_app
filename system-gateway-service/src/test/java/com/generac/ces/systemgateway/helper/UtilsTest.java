package com.generac.ces.systemgateway.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.generac.ces.systemgateway.exception.InternalServerException;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

public class UtilsTest {

    @Test
    public void throwProperError_WebClientResponseException() {
        // Arrange
        WebClientResponseException responseException =
                WebClientResponseException.create(404, "Not Found", null, null, null);

        // Act
        Mono<Object> result = Utils.throwProperError().apply(responseException);

        // Assert
        Assertions.assertThrows(ResponseStatusException.class, result::block);
        ResponseStatusException exception =
                Assertions.assertThrows(ResponseStatusException.class, result::block);
        Assertions.assertEquals(404, exception.getStatus().value());
        Assertions.assertEquals("404 NOT_FOUND \"\"", exception.getMessage());
    }

    @Test
    public void throwProperError_InternalServerException() {
        // Arrange
        Exception exception = new Exception("Something went wrong");

        // Act
        Mono<Object> result = Utils.throwProperError().apply(exception);

        // Assert
        Assertions.assertThrows(InternalServerException.class, result::block);
        InternalServerException internalServerException =
                Assertions.assertThrows(InternalServerException.class, result::block);
        Assertions.assertEquals("Something went wrong", internalServerException.getMessage());
    }

    @Test
    public void logOnError_success() {
        // Arrange
        Signal<Object> signal = Signal.error(new RuntimeException("Test Exception"));
        Consumer<Throwable> errorLogStatement = Mockito.mock(Consumer.class);
        ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);

        // Act
        Utils.logOnError(errorLogStatement).accept(signal);

        // Assert
        verify(errorLogStatement).accept(argumentCaptor.capture());
        Throwable capturedException = argumentCaptor.getValue();
        Assertions.assertEquals("Test Exception", capturedException.getMessage());
    }

    @Test
    public void logOnError_rethrowException() {
        // Arrange
        Signal<Object> signal = Signal.error(new RuntimeException("Test Exception"));
        Consumer<Throwable> errorLogStatement = Mockito.mock(Consumer.class);
        doThrow(new IllegalArgumentException("Error in errorLogStatement"))
                .when(errorLogStatement)
                .accept(any());

        Consumer<Signal<Object>> signalConsumer = Utils.logOnError(errorLogStatement);

        // Act & Assert
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> signalConsumer.accept(signal));
    }
}

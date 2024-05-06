package com.generac.ces.systemgateway.helper;

import com.generac.ces.systemgateway.exception.InternalServerException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

public final class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> Function<Throwable, Mono<T>> throwProperError() {
        return e -> {
            if (e instanceof WebClientResponseException webClientResponseException) {
                return Mono.error(
                        new ResponseStatusException(
                                webClientResponseException.getStatusCode(),
                                webClientResponseException.getResponseBodyAsString()));
            } else {
                return Mono.error(new InternalServerException(e.getMessage()));
            }
        };
    }

    public static <T> Consumer<Signal<T>> logOnError(Consumer<Throwable> errorLogStatement) {
        return signal -> {
            if (signal.isOnError()) {
                String transactionId =
                        (String)
                                signal.getContextView()
                                        .getOrDefault(
                                                "cross_service_transaction_id", (Object) null);
                MDC.MDCCloseable cMdc = MDC.putCloseable("Slf4jMDCFilter.UUID", transactionId);

                try {
                    errorLogStatement.accept(signal.getThrowable());
                } catch (Exception var7) {
                    try {
                        cMdc.close();
                    } catch (Exception var6) {
                        var7.addSuppressed(var6);
                    }
                    throw var7;
                }

                cMdc.close();
            }
        };
    }
}

package com.generac.ces.systemgateway.exception;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@Data
public class TooManyRequestsException extends RuntimeException {
    private final List<String> errorMsgs;
    private final HttpHeaders headers;

    public TooManyRequestsException(String statusText) {
        super(statusText);
        this.errorMsgs = Arrays.asList(statusText);
        this.headers = new HttpHeaders();
    }

    public TooManyRequestsException(List<String> errorMsgs) {
        super();
        this.errorMsgs = errorMsgs;
        this.headers = new HttpHeaders();
    }

    public TooManyRequestsException(String statusText, Duration delaySeconds) {
        super(statusText);
        this.errorMsgs = Arrays.asList(statusText);
        // <delay-seconds>
        // A non-negative decimal integer indicating the seconds to delay after the response is
        // received.
        this.headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(delaySeconds.getSeconds()));
    }

    public TooManyRequestsException(List<String> errorMsgs, Duration delaySeconds) {
        super();
        this.errorMsgs = errorMsgs;
        // <delay-seconds>
        // A non-negative decimal integer indicating the seconds to delay after the response is
        // received.
        this.headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(delaySeconds.getSeconds()));
    }
}

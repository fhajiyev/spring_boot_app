package com.generac.ces.systemgateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class GatewayTimeoutException extends RuntimeException {
    public GatewayTimeoutException(String statusText) {
        super(statusText);
    }
}

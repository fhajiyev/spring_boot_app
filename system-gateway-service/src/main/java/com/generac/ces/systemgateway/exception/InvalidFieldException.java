package com.generac.ces.systemgateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFieldException extends RuntimeException {
    public InvalidFieldException(String statusText) {
        super(statusText);
    }
}

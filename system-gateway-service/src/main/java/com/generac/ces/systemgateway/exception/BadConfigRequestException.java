package com.generac.ces.systemgateway.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Data
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadConfigRequestException extends RuntimeException {
    private final String fieldName;
    private final String errorMessage;

    public BadConfigRequestException(String fieldName, String errorMessage) {
        super();
        this.fieldName = fieldName;
        this.errorMessage = errorMessage;
    }
}

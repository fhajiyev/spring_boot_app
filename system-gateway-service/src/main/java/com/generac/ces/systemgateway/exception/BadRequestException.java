package com.generac.ces.systemgateway.exception;

import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Data
public class BadRequestException extends RuntimeException {
    private final List<String> errorMsgs;

    public BadRequestException(String statusText) {
        super(statusText);
        this.errorMsgs = Arrays.asList(statusText);
    }

    public BadRequestException(List<String> errorMsgs) {
        super();
        this.errorMsgs = errorMsgs;
    }
}

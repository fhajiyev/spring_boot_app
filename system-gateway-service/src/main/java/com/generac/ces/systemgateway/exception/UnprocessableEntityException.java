package com.generac.ces.systemgateway.exception;

import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
@Data
public class UnprocessableEntityException extends RuntimeException {
    private final List<String> errorMsgs;

    public UnprocessableEntityException(String statusText) {
        super(statusText);
        this.errorMsgs = Arrays.asList(statusText);
    }

    public UnprocessableEntityException(List<String> errorMsgs) {
        super();
        this.errorMsgs = errorMsgs;
    }
}

package com.generac.ces.systemgateway.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionResponse implements Serializable {
    private String errorMsg;
    private List<String> errorMsgs;
    private int errorCode;
    private String error;
}

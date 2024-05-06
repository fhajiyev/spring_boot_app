package com.generac.ces.systemgateway.exception;

import com.generac.ces.systemgateway.model.SubscriptionResponse;
import java.util.List;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
@Data
public class SubscriptionInternalServerException extends RuntimeException {
    List<SubscriptionResponse> body;

    public SubscriptionInternalServerException(List<SubscriptionResponse> body) {
        this.body = body;
    }
}

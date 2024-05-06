package com.generac.ces.systemgateway.exception;

import com.generac.ces.systemgateway.model.SubscriptionResponse;
import java.util.List;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Data
public class SubscriptionBadRequestException extends RuntimeException {
    List<SubscriptionResponse> body;

    public SubscriptionBadRequestException(List<SubscriptionResponse> body) {
        this.body = body;
    }
}

package com.generac.ces.systemgateway.helper;

import com.generac.ces.systemgateway.exception.BadRequestException;
import com.generac.ces.systemgateway.exception.GatewayTimeoutException;
import com.generac.ces.systemgateway.exception.InternalServerException;
import com.generac.ces.systemgateway.model.OdinResponse;

public final class ErrorHandlingHelper {

    private ErrorHandlingHelper() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static <T> T handleOdinErrorResponse(OdinResponse response) {
        if ("BAD_REQUEST".equals(response.getCode())) {
            if (response.getEventDetails() instanceof OdinResponse.EventError eventError) {
                throw new BadRequestException(eventError.getEventErrorMessage());
            }
            if (response.getEventDetails() instanceof OdinResponse.EventCanceled eventCanceled) {
                throw new BadRequestException(
                        String.format(
                                "Request to the beacon was canceled. Cancellation type: %s",
                                eventCanceled.getCancelationType()));
            }
            throw new BadRequestException(response.getErrorMessage());
        }
        if ("TIMEOUT".equals(response.getCode())) {
            throw new GatewayTimeoutException(
                    String.format(
                            "Did not receive a response from the device in time, id: %s.",
                            response.getId()));
        }
        throw new InternalServerException(
                String.format("Request id: %s. %s", response.getId(), response.getErrorMessage()));
    }
}

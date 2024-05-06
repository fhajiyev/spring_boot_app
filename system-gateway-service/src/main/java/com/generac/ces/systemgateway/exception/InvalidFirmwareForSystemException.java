package com.generac.ces.systemgateway.exception;

import java.util.UUID;

public class InvalidFirmwareForSystemException extends BadRequestException {
    public InvalidFirmwareForSystemException(UUID systemId, String updateVersion) {
        super(
                "System %s is not compatible with requested update version %s."
                        .formatted(systemId, updateVersion));
    }
}

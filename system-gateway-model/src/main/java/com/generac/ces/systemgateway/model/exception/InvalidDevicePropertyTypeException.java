package com.generac.ces.systemgateway.model.exception;

public class InvalidDevicePropertyTypeException extends Exception {
    public InvalidDevicePropertyTypeException(String type) {
        super("Invalid device property type: %s.".formatted(type));
    }
}

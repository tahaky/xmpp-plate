package com.xmpp.plate.exception;

/**
 * Exception thrown when a vehicle already exists
 */
public class VehicleAlreadyExistsException extends RuntimeException {
    
    public VehicleAlreadyExistsException(String message) {
        super(message);
    }
    
    public VehicleAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

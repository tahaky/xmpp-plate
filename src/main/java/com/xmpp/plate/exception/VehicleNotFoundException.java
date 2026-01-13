package com.xmpp.plate.exception;

/**
 * Exception thrown when a vehicle is not found
 */
public class VehicleNotFoundException extends RuntimeException {
    
    public VehicleNotFoundException(String message) {
        super(message);
    }
    
    public VehicleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

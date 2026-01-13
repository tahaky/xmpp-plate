package com.xmpp.plate.exception;

/**
 * Exception thrown when XMPP operations fail
 */
public class XmppOperationException extends RuntimeException {
    
    public XmppOperationException(String message) {
        super(message);
    }
    
    public XmppOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

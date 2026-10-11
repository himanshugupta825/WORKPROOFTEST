package com.workproof.exception;

/**
 * Wraps SQLException so DAO code does not leak JDBC details to the web layer.
 * The message is safe for logs; it never contains SQL parameters or credentials.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}

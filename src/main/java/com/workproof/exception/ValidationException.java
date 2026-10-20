package com.workproof.exception;

/**
 * Thrown when user input fails server-side validation.
 * The message is written for the end user and is safe to display.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.workproof.exception;

/**
 * Thrown when a logged-in user tries an action they are not allowed to perform.
 * Also thrown for the "no self-approval" rule.
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }
}

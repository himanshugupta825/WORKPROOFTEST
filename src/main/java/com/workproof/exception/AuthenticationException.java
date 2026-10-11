package com.workproof.exception;

/** Wrong credentials or a disabled account. */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}

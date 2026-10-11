package com.workproof.exception;

/** Requested record does not exist (or must not be revealed to this user). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

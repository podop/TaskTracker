package com.tasktracker.exception;

public class IllegalTransitionException extends RuntimeException {
    public IllegalTransitionException(String message) {
        super(message);
    }
}

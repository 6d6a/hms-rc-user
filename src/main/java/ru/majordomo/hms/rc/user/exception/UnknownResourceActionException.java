package ru.majordomo.hms.rc.user.exception;

public class UnknownResourceActionException extends RuntimeException {
    public UnknownResourceActionException(String message) {
        super(message);
    }
}

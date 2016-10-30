package ru.majordomo.hms.rc.user.exception;

public class ParameterValidateException extends RuntimeException {
    public ParameterValidateException() {};
    public ParameterValidateException(String message) {
        super(message);
    }
}

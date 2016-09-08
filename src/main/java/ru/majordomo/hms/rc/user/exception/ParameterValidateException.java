package ru.majordomo.hms.rc.user.exception;

public class ParameterValidateException extends Exception {
    public ParameterValidateException() {};
    public ParameterValidateException(String message) {
        super(message);
    }
}

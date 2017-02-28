package ru.majordomo.hms.rc.user.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Ресурс не найден");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

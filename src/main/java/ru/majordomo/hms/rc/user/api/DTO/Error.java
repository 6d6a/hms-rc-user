package ru.majordomo.hms.rc.user.api.DTO;

import lombok.Data;

import java.util.List;

@Data
public class Error {
    private final String property;
    private final List<String> errors;
}

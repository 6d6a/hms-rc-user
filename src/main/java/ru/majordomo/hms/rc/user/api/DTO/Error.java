package ru.majordomo.hms.rc.user.api.DTO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Error {
    private String property;
    private List<String> errors = new ArrayList<>();
}

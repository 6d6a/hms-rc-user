package ru.majordomo.hms.rc.user.api.DTO;

import java.util.List;

import lombok.Data;

@Data
public class FieldWithStringsContainer {
    private String field;
    private List<String> strings;
}

package ru.majordomo.hms.rc.user.api.DTO.stat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseResourceIdCount extends BaseCount {
    private String resourceId;
    private boolean active;
    private String name;
}


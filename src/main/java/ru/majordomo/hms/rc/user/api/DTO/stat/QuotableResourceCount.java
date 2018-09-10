package ru.majordomo.hms.rc.user.api.DTO.stat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuotableResourceCount extends BaseResourceIdCount {
    private Long quotaUsed;
}

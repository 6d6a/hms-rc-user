package ru.majordomo.hms.rc.user.api.DTO.stat;

import lombok.Data;

@Data
public class ResourceQuotaCount {
    private String accountId;
    private Long quotaUsed = 0L;
    private int count = 0;
}

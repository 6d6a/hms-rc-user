package ru.majordomo.hms.rc.user.resources;

public interface Quotable {
    void setQuota(Long quota);
    Long getQuota();
    void setQuotaUsed(Long quotaUsed);
    Long getQuotaUsed();
    void setWritable(Boolean writable);
    Boolean getWritable();
}

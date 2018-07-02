package ru.majordomo.hms.rc.user.api.DTO;

public class Count {
    private final Long count;

    public Count(Long count) {
        this.count = count;
    }

    public Long getCount() {
        return count;
    }

    public static Count zero() {
        return new Count(0L);
    }
}

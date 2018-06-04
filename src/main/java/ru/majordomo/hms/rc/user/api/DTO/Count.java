package ru.majordomo.hms.rc.user.api.DTO;

public class Count {
    private Long count;

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public static Count zero() {
        Count count = new Count();
        count.setCount(0L);
        return count;
    }
}

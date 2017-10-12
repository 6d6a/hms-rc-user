package ru.majordomo.hms.rc.user.event.quota;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.Quotable;

public class QuotaAlreadyFullEvent extends ApplicationEvent{

    public QuotaAlreadyFullEvent(Quotable source) {
        super(source);
    }

    @Override
    public Quotable getSource() {
        return (Quotable) super.getSource();
    }
}

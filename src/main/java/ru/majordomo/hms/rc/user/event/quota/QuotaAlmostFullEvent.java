package ru.majordomo.hms.rc.user.event.quota;

import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.rc.user.resources.Quotable;

public class QuotaAlmostFullEvent extends ApplicationEvent{

    public QuotaAlmostFullEvent(Quotable source) {
        super(source);
    }

    @Override
    public Quotable getSource() {
        return (Quotable) super.getSource();
    }
}

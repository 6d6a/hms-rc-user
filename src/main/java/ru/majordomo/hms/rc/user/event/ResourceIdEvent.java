package ru.majordomo.hms.rc.user.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;


public abstract class ResourceIdEvent extends ApplicationEvent {
    public ResourceIdEvent(String resourceId) {
        super(resourceId);
    }

    @Override
    public String getSource() {
        Assert.isInstanceOf(String.class, source, "resourceId must be String");
        return (String) source;
    }
}

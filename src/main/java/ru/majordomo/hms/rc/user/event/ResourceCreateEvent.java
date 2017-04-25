package ru.majordomo.hms.rc.user.event;


import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.Resource;

public abstract class ResourceCreateEvent<T extends Resource> extends ApplicationEvent {
    public ResourceCreateEvent(T source) {
        super(source);
    }

    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}

package ru.majordomo.hms.rc.user.event;


import org.springframework.context.ApplicationEvent;

public abstract class ResourceImportEvent extends ApplicationEvent {
    public ResourceImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}

package ru.majordomo.hms.rc.user.event.person;

import org.springframework.context.ApplicationEvent;

public class PersonImportEvent extends ApplicationEvent {
    public PersonImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}

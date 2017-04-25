package ru.majordomo.hms.rc.user.event.person;

import ru.majordomo.hms.rc.user.event.ResourceImportEvent;

public class PersonImportEvent extends ResourceImportEvent {
    public PersonImportEvent(String source) {
        super(source);
    }
}

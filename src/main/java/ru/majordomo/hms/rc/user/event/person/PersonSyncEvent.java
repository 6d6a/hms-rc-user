package ru.majordomo.hms.rc.user.event.person;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.Person;

public class PersonSyncEvent extends ResourceCreateEvent<Person> {
    public PersonSyncEvent(Person source) {
        super(source);
    }
}

package ru.majordomo.hms.rc.user.event.person;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.Person;

public class PersonCreateEvent extends ResourceCreateEvent<Person> {
    public PersonCreateEvent(Person source) {
        super(source);
    }
}

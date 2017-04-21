package ru.majordomo.hms.rc.user.event.person;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.Person;

public class PersonCreateEvent extends ApplicationEvent {
    public PersonCreateEvent(Person source) {
        super(source);
    }

    @Override
    public Person getSource() {
        return (Person) super.getSource();
    }
}

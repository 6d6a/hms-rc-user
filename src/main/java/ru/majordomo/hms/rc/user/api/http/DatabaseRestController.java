package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;

@RestController(value = "/database")
public class DatabaseRestController {

    private GovernorOfDatabase governor;

    @Autowired
    public void setGovernor(GovernorOfDatabase governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/{databaseId}", "/{databaseId}/"}, method = RequestMethod.GET)
    public Person readOne(@PathVariable String databaseId) {
        return (Person) governor.build(databaseId);
    }

    @RequestMapping(value = {"/",""}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

}

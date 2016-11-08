package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;

@RestController
public class PersonRestController {

    private GovernorOfPerson governor;

    @Autowired
    public void setGovernor(GovernorOfPerson governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/person/{personId}", "/person/{personId}/"}, method = RequestMethod.GET)
    public Person readOne(@PathVariable String personId) {
        return (Person) governor.build(personId);
    }

    @RequestMapping(value = {"/person/","/person"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/person", "/{accountId}/person/"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

}

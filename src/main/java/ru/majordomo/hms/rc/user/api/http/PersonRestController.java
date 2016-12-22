package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @RequestMapping(value = {"{accountId}/person/{personId}", "{accountId}/person/{personId}/"}, method = RequestMethod.GET)
    public Person readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("personId") String personId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", personId);
        keyValue.put("accountId", accountId);
        return (Person) governor.build(keyValue);
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

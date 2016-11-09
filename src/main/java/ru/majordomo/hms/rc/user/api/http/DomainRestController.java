package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;

@RestController
public class DomainRestController {

    private GovernorOfDomain governor;

    @Autowired
    public void setGovernor(GovernorOfDomain governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/domain/{domainId}", "/domain/{domainId}/"}, method = RequestMethod.GET)
    public Domain readOne(@PathVariable String domainId) {
        return (Domain) governor.build(domainId);
    }

    @RequestMapping(value = {"{accountId}/domain/{domainId}", "{accountId}/domain/{domainId}/"}, method = RequestMethod.GET)
    public Domain readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("domainId") String domainId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("domainId", domainId);
        keyValue.put("accountId", accountId);
        return (Domain) governor.build(keyValue);
    }

    @RequestMapping(value = {"/domain/","/domain"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/domain", "/{accountId}/domain/"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

}

package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.Resource;

@RestController
public class DatabaseRestController {

    private GovernorOfDatabase governor;

    @Autowired
    public void setGovernor(GovernorOfDatabase governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/database/{databaseId}", "/database/{databaseId}/"}, method = RequestMethod.GET)
    public Database readOne(@PathVariable String databaseId) {
        return (Database) governor.build(databaseId);
    }

    @RequestMapping(value = {"{accountId}/database/{databaseId}", "{accountId}/database/{databaseId}/"}, method = RequestMethod.GET)
    public Database readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("databaseId") String databaseId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("databaseId", databaseId);
        keyValue.put("accountId", accountId);
        return (Database) governor.build(keyValue);
    }

    @RequestMapping(value = {"/database/","/database"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/database", "/{accountId}/database/"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/{accountId}/database/count", "/{accountId}/database/count/"}, method = RequestMethod.GET)
    public Count countByAccountId(@PathVariable String accountId) {
        return governor.countByAccountId(accountId);
    }

}

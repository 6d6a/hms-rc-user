package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

@RestController
public class DatabaseUserRestController {

    private GovernorOfDatabaseUser governor;

    @Autowired
    public void setGovernor(GovernorOfDatabaseUser governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/database-user/{databaseUserId}", "/database-user/{databaseUserId}/"}, method = RequestMethod.GET)
    public DatabaseUser readOne(@PathVariable String databaseUserId) {
        return governor.build(databaseUserId);
    }

    @RequestMapping(value = {"{accountId}/database-user/{databaseUserId}", "{accountId}/database-user/{databaseUserId}/"}, method = RequestMethod.GET)
    public DatabaseUser readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("databaseUserId") String databaseUserId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", databaseUserId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/database-user/","/database-user"}, method = RequestMethod.GET)
    public Collection<DatabaseUser> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/database-user", "/{accountId}/database-user/"}, method = RequestMethod.GET)
    public Collection<DatabaseUser> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

}

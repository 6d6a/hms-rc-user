package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/database-user/{databaseUserId}")
    public DatabaseUser readOne(@PathVariable String databaseUserId) {
        return governor.build(databaseUserId);
    }

    @GetMapping("{accountId}/database-user/{databaseUserId}")
    public DatabaseUser readOneByAccountId(
            @PathVariable("accountId") String accountId,
            @PathVariable("databaseUserId") String databaseUserId
    ) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", databaseUserId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/database-user")
    public Collection<DatabaseUser> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/database-user")
    public Collection<DatabaseUser> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @GetMapping("/{accountId}/database-user/filter")
    public Collection<DatabaseUser> filterByAccountId(
            @PathVariable String accountId,
            @RequestParam Map<String, String> requestParams
    ) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @GetMapping("/database-user/filter")
    public Collection<DatabaseUser> filter(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }
}

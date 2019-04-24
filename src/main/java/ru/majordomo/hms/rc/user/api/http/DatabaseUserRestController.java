package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.configurations.MysqlSessionVariablesConfig;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;

@RestController
public class DatabaseUserRestController {

    private GovernorOfDatabaseUser governor;
    private MysqlSessionVariablesConfig mysqlSessionVariablesConfig;

    @Autowired
    public void setGovernor(GovernorOfDatabaseUser governor) {
        this.governor = governor;
    }

    @Autowired
    public void setMysqlSessionVariablesConfig(MysqlSessionVariablesConfig mysqlSessionVariablesConfig) {
        this.mysqlSessionVariablesConfig = mysqlSessionVariablesConfig;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/database-user/{databaseUserId}")
    public DatabaseUser readOne(@PathVariable String databaseUserId) {
        return governor.build(databaseUserId);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/database-user")
    public Collection<DatabaseUser> readAll() {
        return governor.buildAll();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/database-user")
    public Collection<DatabaseUser> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/database-user/filter")
    public Collection<DatabaseUser> filterByAccountId(
            @PathVariable String accountId,
            @RequestParam Map<String, String> requestParams
    ) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/database-user/filter")
    public Collection<DatabaseUser> filter(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping(
            {
                    "{accountId}/database-user/session-variables/collations",
                    "/database-user/session-variables/collations"
            })
    public List<String> collations(
            @PathVariable(value = "accountId", required = false) String accountId
    ) {
       return mysqlSessionVariablesConfig.getCollations();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping(
            {
                    "{accountId}/database-user/session-variables/query-cache-types",
                    "/database-user/session-variables/query-cache-types"
            })
    public List<String> queryCacheTypes(
            @PathVariable(value = "accountId", required = false) String accountId
    ) {
        return mysqlSessionVariablesConfig.getQueryCacheTypes();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping(
            {
                    "{accountId}/database-user/session-variables/charsets",
                    "/database-user/session-variables/charsets"
            })
    public List<String> charsets(
            @PathVariable(value = "accountId", required = false) String accountId
    ) {
        return mysqlSessionVariablesConfig.getCharsets();
    }
}

package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.Database;

@RestController
public class DatabaseRestController {

    private GovernorOfDatabase governor;

    @Autowired
    public void setGovernor(GovernorOfDatabase governor) {
        this.governor = governor;
    }

    @GetMapping("/{accountId}/database/filter")
    public Collection<Database> filterByAccountId(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @GetMapping("/database/filter")
    public Collection<Database> filter(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }

    @GetMapping("/database/{databaseId}")
    public Database readOne(@PathVariable String databaseId) {
        return governor.build(databaseId);
    }

    @GetMapping("{accountId}/database/{databaseId}")
    public Database readOneByAccountId(
            @PathVariable("accountId") String accountId,
            @PathVariable("databaseId") String databaseId
    ) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", databaseId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/database")
    public Collection<Database> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/database")
    public Collection<Database> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @GetMapping("/{accountId}/database/count")
    public Count countByAccountId(@PathVariable String accountId) {
        return governor.countByAccountId(accountId);
    }

    @PostMapping({"/database/{databaseId}/quota-report"})
    public ResponseEntity<Void> updateQuota(
            @PathVariable String databaseId,
            @RequestBody QuotaReport report
    ) {
        try {
            governor.updateQuota(databaseId, report.getQuotaUsed());
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}

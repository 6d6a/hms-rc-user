package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.MalwareReport;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@RestController
public class UnixAccountRESTController {

    private GovernorOfUnixAccount governor;

    @Autowired
    public void setGovernor(GovernorOfUnixAccount governor) {
        this.governor = governor;
    }

    @GetMapping("/unix-account/{unixAccountId}")
    public UnixAccount readOne(@PathVariable String unixAccountId) {
        return governor.build(unixAccountId);
    }

    @GetMapping("{accountId}/unix-account/{unixAccountId}")
    public UnixAccount readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("unixAccountId") String unixAccountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", unixAccountId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/unix-account")
    public Collection<UnixAccount> readAll() {
        return governor.buildAll();
    }

    @GetMapping(value = "/unix-account/filter", headers = {"X-HMS-Projection=pm"})
    public Collection<UnixAccount> filterForPm(@RequestParam Map<String, String> keyValue) {
        return governor.buildAllPm(keyValue);
    }

    @GetMapping("/unix-account/filter")
    public Collection<UnixAccount> filter(@RequestParam Map<String, String> keyValue) {
        return governor.buildAll(keyValue);
    }

    @PostMapping("/unix-account/{unixAccountId}/quota-report")
    public ResponseEntity<Void> updateQuota(@PathVariable String unixAccountId, @RequestBody QuotaReport report) {
        try {
            governor.updateQuotaUsed(unixAccountId, report.getQuotaUsed());
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("{accountId}/unix-account/{unixAccountId}/malware-report")
    public MalwareReport getMalwareReport(@PathVariable String accountId, @PathVariable String unixAccountId) {
        return governor.getMalwareReport(accountId, unixAccountId);
    }

    @PostMapping("/unix-account/{unixAccountId}/malware-report")
    public ResponseEntity<Void> reportMalware(@PathVariable String unixAccountId, @RequestBody MalwareReport report) {
        report.setUnixAccountId(unixAccountId);
        try {
            governor.processMalwareReport(report);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{accountId}/unix-account")
    public Collection<UnixAccount> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }
}

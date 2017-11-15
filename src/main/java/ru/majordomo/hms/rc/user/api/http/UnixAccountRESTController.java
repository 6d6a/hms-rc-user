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

    @RequestMapping(value = {"/unix-account/{unixAccountId}", "/unix-account/{unixAccountId}/"}, method = RequestMethod.GET)
    public UnixAccount readOne(@PathVariable String unixAccountId) {
        return governor.build(unixAccountId);
    }

    @RequestMapping(value = {"{accountId}/unix-account/{unixAccountId}", "{accountId}/unix-account/{unixAccountId}/"}, method = RequestMethod.GET)
    public UnixAccount readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("unixAccountId") String unixAccountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", unixAccountId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/unix-account/","/unix-account"}, method = RequestMethod.GET)
    public Collection<UnixAccount> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/unix-account/filter"}, method = RequestMethod.GET)
    public Collection<UnixAccount> filter(@RequestParam Map<String, String> keyValue) {
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/unix-account/{unixAccountId}/quota-report"}, method = RequestMethod.POST)
    public ResponseEntity<Void> updateQuota(@PathVariable String unixAccountId, @RequestBody QuotaReport report) {
        try {
            governor.updateQuotaUsed(unixAccountId, report.getQuotaUsed());
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = {"/unix-account/{unixAccountId}/malware-report"}, method = RequestMethod.GET)
    public MalwareReport getMalwareReport(@PathVariable String unixAccountId) {
        return governor.getMalwareReport(unixAccountId);
    }

    @RequestMapping(value = {"/unix-account/{unixAccountId}/malware-report"}, method = RequestMethod.POST)
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

    @RequestMapping(value = {"/unix-account/{unixAccountId}/solve-malware-report"}, method = RequestMethod.GET)
    public ResponseEntity<Void> confirmSolved(@PathVariable String unixAccountId) {
        try {
            governor.solveMalwareReport(unixAccountId);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = {"/{accountId}/unix-account", "/{accountId}/unix-account/"}, method = RequestMethod.GET)
    public Collection<UnixAccount> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }
}

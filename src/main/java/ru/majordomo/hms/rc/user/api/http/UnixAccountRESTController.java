package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.Resource;
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
        return (UnixAccount) governor.build(unixAccountId);
    }

    @RequestMapping(value = {"{accountId}/unix-account/{unixAccountId}", "{accountId}/unix-account/{unixAccountId}/"}, method = RequestMethod.GET)
    public UnixAccount readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("unixAccountId") String unixAccountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", unixAccountId);
        keyValue.put("accountId", accountId);
        return (UnixAccount) governor.build(keyValue);
    }

    @RequestMapping(value = {"/unix-account/{unixAccountId}", "/unix-account/{unixAccountId}/"}, method = RequestMethod.PATCH)
    public ResponseEntity updateUnixAccount(@PathVariable("unixAccountId") String unixAccountId, @RequestBody Map<String, Object> keyValue) {
        try {
            for (Map.Entry<String, Object> entry : keyValue.entrySet()) {
                switch (entry.getKey()) {
                    case "sendmailAllowed":
                        governor.updateSendmailAllowed(unixAccountId, (Boolean) entry.getValue());
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }
        return new ResponseEntity(HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = {"/unix-account/","/unix-account"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/unix-account/filter"}, method = RequestMethod.GET)
    public Collection<? extends Resource> filter(@RequestParam Map<String, String> keyValue) {
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/unix-account/{unixAccountId}/quota-report"}, method = RequestMethod.POST)
    public ResponseEntity<?> updateQuota(@PathVariable String unixAccountId, @RequestBody QuotaReport report) {
        try {
            governor.updateQuota(unixAccountId, report.getQuotaUsed());
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = {"/{accountId}/unix-account", "/{accountId}/unix-account/"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }
}

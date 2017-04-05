package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.Mailbox;

@RestController
public class MailboxRestController {

    private GovernorOfMailbox governor;

    @Autowired
    public void setGovernor(GovernorOfMailbox governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/mailbox/{mailboxId}", "/mailbox/{mailboxId}/"}, method = RequestMethod.GET)
    public Mailbox readOne(@PathVariable String mailboxId) {
        return governor.build(mailboxId);
    }

    @RequestMapping(value = {"{accountId}/mailbox/{mailboxId}", "{accountId}/mailbox/{mailboxId}/"}, method = RequestMethod.GET)
    public Mailbox readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("mailboxId") String mailboxId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", mailboxId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/mailbox/","/mailbox"}, method = RequestMethod.GET)
    public Collection<Mailbox> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/mailbox", "/{accountId}/mailbox/"}, method = RequestMethod.GET)
    public Collection<Mailbox> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/mailbox/filter"}, method = RequestMethod.GET)
    public Collection<Mailbox> filter(@RequestParam Map<String, String> keyValue) {
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/mailbox/{mailboxId}/quota-report"}, method = RequestMethod.POST)
    public ResponseEntity<Void> updateQuota(@PathVariable String mailboxId, @RequestBody QuotaReport report) {
        try {
            governor.updateQuota(mailboxId, report.getQuotaUsed());
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}

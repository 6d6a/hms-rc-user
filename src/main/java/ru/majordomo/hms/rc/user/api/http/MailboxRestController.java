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

    @GetMapping("/mailbox/{mailboxId}")
    public Mailbox readOne(@PathVariable String mailboxId) {
        return governor.build(mailboxId);
    }

    @GetMapping("{accountId}/mailbox/{mailboxId}")
    public Mailbox readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("mailboxId") String mailboxId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", mailboxId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/mailbox")
    public Collection<Mailbox> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/mailbox")
    public Collection<Mailbox> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @GetMapping(value = {"/mailbox/filter"}, headers = {"X-HMS-Projection=te"})
    public Collection<Mailbox> filterTe(@RequestParam Map<String, String> keyValue) {
        return governor.buildAllForTe(keyValue);
    }

    @GetMapping("/mailbox/filter")
    public Collection<Mailbox> filter(@RequestParam Map<String, String> keyValue) {
        return governor.buildAll(keyValue);
    }

    @PostMapping("/mailbox/{mailboxId}/quota-report")
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

package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.DTO.stat.ResourceQuotaCount;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.Utils;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.resources.DTO.QuotaReport;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.service.stat.Aggregator;

@RestController
public class MailboxRestController {

    private GovernorOfMailbox governor;

    private Aggregator aggregator;

    @Autowired
    public void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Autowired
    public void setGovernor(GovernorOfMailbox governor) {
        this.governor = governor;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/mailbox/{mailboxId}")
    public Mailbox readOne(@PathVariable String mailboxId) {
        return governor.build(mailboxId);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/mailbox/{mailboxId}")
    public Mailbox readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("mailboxId") String mailboxId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", mailboxId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/mailbox")
    public Collection<Mailbox> readAll() {
        return governor.buildAll();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/mailbox")
    public Collection<Mailbox> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping(value = {"/mailbox/filter"}, headers = {"X-HMS-Projection=te"})
    public Collection<Mailbox> filterTe(@RequestParam Map<String, String> keyValue) {
        return governor.buildAllForTe(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/mailbox/filter")
    public Collection<Mailbox> filter(@RequestParam Map<String, String> keyValue) {
        return governor.buildAll(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/mailbox/quota-count")
    public ResourceQuotaCount getCountByAccountId(@PathVariable String accountId) {
        return aggregator.getResourceQuotaCountByAccountId(Mailbox.class, accountId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mailbox/redis/sync")
    public ResponseEntity<ServiceMessage> sync() {
        governor.syncAll();
        return ResponseEntity.accepted().body(Utils.makeSuccessResponse());
    }
}

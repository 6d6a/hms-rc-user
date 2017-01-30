package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;

@RestController
public class MailboxRestController {

    private GovernorOfMailbox governor;

    @Autowired
    public void setGovernor(GovernorOfMailbox governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/mailbox/{mailboxId}", "/mailbox/{mailboxId}/"}, method = RequestMethod.GET)
    public Mailbox readOne(@PathVariable String mailboxId) {
        return (Mailbox) governor.build(mailboxId);
    }

    @RequestMapping(value = {"{accountId}/mailbox/{mailboxId}", "{accountId}/mailbox/{mailboxId}/"}, method = RequestMethod.GET)
    public Mailbox readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("mailboxId") String mailboxId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", mailboxId);
        keyValue.put("accountId", accountId);
        return (Mailbox) governor.build(keyValue);
    }

    @RequestMapping(value = {"/mailbox/","/mailbox"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/mailbox", "/{accountId}/mailbox/"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/mailbox/filter"}, method = RequestMethod.GET)
    public Collection<? extends Resource> filter(@RequestParam Map<String, String> keyValue) {
        return governor.buildAll(keyValue);
    }

}

package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;

@RestController(value = "/mailbox")
@CrossOrigin("*")
public class MailboxRestController {

    private GovernorOfMailbox governor;

    @Autowired
    public void setGovernor(GovernorOfMailbox governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/{mailboxId}", "/{mailboxId}/"}, method = RequestMethod.GET)
    public Mailbox readOne(@PathVariable String mailboxId) {
        return (Mailbox) governor.build(mailboxId);
    }

    @RequestMapping(value = {"/",""}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

}

package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
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

    @RequestMapping(value = {"/unix-account/","/unix-account"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/unix-account/{unixAccountId}/quota/{quotaSize}"}, method = RequestMethod.POST)
    public ResponseEntity<?> updateQuota() {
        return new ResponseEntity<Object>(HttpStatus.ACCEPTED);
    }
}

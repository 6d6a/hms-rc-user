package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@RestController
@CrossOrigin("*")
public class UnixAccountRESTController {
    @Autowired
    UnixAccountRepository unixAccountRepository;

    @RequestMapping("/rc/unixaccount/{id}")
    public UnixAccount readOne(@PathVariable String id) {
        return unixAccountRepository.findOne(id);
    }

    @RequestMapping("/rc/unixaccount")
    public Collection<UnixAccount> readAll() {
        return unixAccountRepository.findAll();
    }
}

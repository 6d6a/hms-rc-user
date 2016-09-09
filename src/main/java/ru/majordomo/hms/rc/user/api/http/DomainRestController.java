package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

@RestController
@CrossOrigin("*")
public class DomainRestController {

    @Autowired
    DomainRepository domainRepository;

    @RequestMapping(value = "/rc/domain/{domainId}", method = RequestMethod.GET)
    public Domain readOne(@PathVariable String domainId) {
        return domainRepository.findOne(domainId);
    }

    @RequestMapping(value = "/rc/domain", method = RequestMethod.GET)
    public Collection<Domain> readAll() {
        return domainRepository.findAll();
    }
}

package ru.majordomo.hms.rc.user.api.interfaces;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.RegSpec;

@FeignClient("DOMAIN-REGISTRAR")
public interface DomainRegistrarClient {
    @RequestMapping(value = "/person/{nicHandle}/domain/{domainName}", method = RequestMethod.POST)
    ResponseEntity registerDomain(@PathVariable("nicHandle") String nicHandle, @PathVariable("domainName") String domainName);

    @RequestMapping(value = "/person/{nicHandle}/domain/{domainName}/renew", method = RequestMethod.PUT)
    ResponseEntity renewDomain(@PathVariable("nicHandle") String nicHandle, @PathVariable("domainName") String domainName);

    @RequestMapping(value = "/person", method = RequestMethod.POST)
    ResponseEntity createPerson(@RequestBody Person person);

    @RequestMapping(value = "/person/{nicHandle}", method = RequestMethod.PATCH)
    ResponseEntity updatePerson(@PathVariable("nicHandle") String nicHandle, @RequestBody Person person);

    @RequestMapping(value = "/person/{nicHandle}", method = RequestMethod.GET)
    Person getPerson(@PathVariable("nicHandle") String nicHandle);

    @RequestMapping(value = "/domain/{domainName}/reg-spec", method = RequestMethod.GET)
    RegSpec getRegSpec(@PathVariable("domainName") String domainName);
}

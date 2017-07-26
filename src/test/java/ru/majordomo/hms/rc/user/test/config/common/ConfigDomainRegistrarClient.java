package ru.majordomo.hms.rc.user.test.config.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;
import ru.majordomo.hms.rc.user.resources.DomainState;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.RegSpec;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ConfigDomainRegistrarClient {

    @Bean
    public DomainRegistrarClient domainRegistrarClient() {
        return new DomainRegistrarClient() {
            @Override
            public ResponseEntity registerDomain(@PathVariable("nicHandle") String nicHandle, @PathVariable("domainName") String domainName) {
                return new ResponseEntity(HttpStatus.CREATED);
            }

            @Override
            public ResponseEntity renewDomain(@PathVariable("domainName") String domainName, @PathVariable("registrar") DomainRegistrar registrar) {
                return new ResponseEntity(HttpStatus.CREATED);
            }

            @Override
            public ResponseEntity createPerson(@RequestBody Person person) {
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create("http://domain-registrar/person/RGS-000001"));
                return new ResponseEntity(headers, HttpStatus.CREATED);
            }

            @Override
            public ResponseEntity updatePerson(@PathVariable("nicHandle") String nicHandle, @RequestBody Person person) {
                return new ResponseEntity(HttpStatus.CREATED);
            }

            @Override
            public Person getPerson(@PathVariable("nicHandle") String nicHandle) {
                return new Person();
            }

            @Override
            public RegSpec getRegSpec(@PathVariable("domainName") String domainName) {
                RegSpec regSpec = new RegSpec();
                regSpec.addState(DomainState.NOT_DELEGATED);
                regSpec.addState(DomainState.VERIFIED);
                return regSpec;
            }
        };
    }

}

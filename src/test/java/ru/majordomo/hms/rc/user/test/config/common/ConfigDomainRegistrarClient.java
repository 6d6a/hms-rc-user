package ru.majordomo.hms.rc.user.test.config.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.majordomo.hms.rc.user.api.interfaces.DomainRegistrarClient;
import ru.majordomo.hms.rc.user.resources.DomainState;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.RegSpec;

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
            public ResponseEntity createPerson(@RequestBody Person person) {
                return new ResponseEntity(HttpStatus.CREATED);
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

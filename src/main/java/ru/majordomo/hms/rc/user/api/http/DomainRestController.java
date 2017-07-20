package ru.majordomo.hms.rc.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.IDN;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.event.domain.DomainClearSyncEvent;
import ru.majordomo.hms.rc.user.event.domain.DomainSyncEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfDnsRecord;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.RegSpec;

@RestController
public class DomainRestController {

    private GovernorOfDomain governor;
    private ApplicationEventPublisher publisher;

    @Autowired
    private GovernorOfDnsRecord governorOfDnsRecord;

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    private final static Logger logger = LoggerFactory.getLogger(DomainRestController.class);

    @Autowired
    public void setGovernor(GovernorOfDomain governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/domain/{domainId}", "/domain/{domainId}/"}, method = RequestMethod.GET)
    public Domain readOne(@PathVariable String domainId) {
        return governor.build(domainId);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/domain/{domain-name}/add-dns-record")
    public ResponseEntity<DNSResourceRecord> addDnsRecord(@PathVariable("domain-name") String domainName, @RequestBody DNSResourceRecord record) {
        try {
            Map<String, String> keyValue = new HashMap<>();
            keyValue.put("name", domainName);
            logger.info("adding DNS-record for domain " + domainName);
            Domain domain = governor.build(keyValue);
            if (domain.getParentDomainId() != null)
                domain = governor.build(domain.getParentDomainId());
            record.setName(IDN.toASCII(domain.getName()));
            governorOfDnsRecord.validate(record);
            governorOfDnsRecord.store(record);
            return new ResponseEntity<>(record, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(new DNSResourceRecord(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/domain/process-sync")
    public ResponseEntity<Void> processDomainsSync(@RequestBody Map<String, RegSpec> domains) {
        try {
            //Синхронизируем существующие
            domains.forEach((key, value) -> publisher.publishEvent(new DomainSyncEvent(key, value)));

            //Удаляем reg-spec у необновляющихся более 4 часов.
            publisher.publishEvent(new DomainClearSyncEvent("DomainClearSyncEvent"));

            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/domain/{domain-name}/delete-dns-record")
    public ResponseEntity deleteDnsRecord(@PathVariable("domain-name") String domainName, @RequestBody DNSResourceRecord record) {
        try {
            governorOfDnsRecord.drop(record.getRecordId().toString());
            return new ResponseEntity(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = {"{accountId}/domain/{domainId}", "{accountId}/domain/{domainId}/"}, method = RequestMethod.GET)
    public Domain readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("domainId") String domainId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", domainId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/domain/","/domain"}, method = RequestMethod.GET)
    public Collection<Domain> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/domain", "/{accountId}/domain/"}, method = RequestMethod.GET)
    public Collection<Domain> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/domain/filter"}, method = RequestMethod.GET)
    public Collection<Domain> readAllWithParams(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }

    @RequestMapping(value = {"{accountId}/domain/filter"}, method = RequestMethod.GET)
    public Collection<Domain> readAllWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @RequestMapping(value = {"/domain/find"}, method = RequestMethod.GET)
    public Domain readOneWithParams(@RequestParam Map<String, String> requestParams) {
        return governor.build(requestParams);
    }

    @RequestMapping(value = {"{accountId}/domain/find"}, method = RequestMethod.GET)
    public Domain readOneWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.build(requestParams);
    }

}

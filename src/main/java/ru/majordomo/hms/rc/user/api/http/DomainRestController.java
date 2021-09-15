package ru.majordomo.hms.rc.user.api.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.IDN;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.DTO.DomainSyncReport;
import ru.majordomo.hms.rc.user.api.interfaces.PmFeignClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.domain.DomainClearSyncEvent;
import ru.majordomo.hms.rc.user.event.domain.RegSpecUpdateEvent;
import ru.majordomo.hms.rc.user.managers.GovernorOfDnsRecord;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;
import ru.majordomo.hms.rc.user.resources.RegSpec;

@RestController
public class DomainRestController {

    private GovernorOfDomain governor;
    private ApplicationEventPublisher publisher;
    private GovernorOfDnsRecord governorOfDnsRecord;
    private PmFeignClient pmFeignClient;
    private final static Logger logger = LoggerFactory.getLogger(DomainRestController.class);

    @Autowired
    public void setGovernorOfDnsRecord(GovernorOfDnsRecord governorOfDnsRecord) {
        this.governorOfDnsRecord = governorOfDnsRecord;
    }

    @Autowired
    public void setPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Autowired
    public void setGovernor(GovernorOfDomain governor) {
        this.governor = governor;
    }

    @Autowired
    public void setPmFeignClient(PmFeignClient pmFeignClient) {
        this.pmFeignClient = pmFeignClient;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/domain/{domainId}")
    public Domain readOne(@PathVariable String domainId) {
        return governor.build(domainId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/domain/{domain-name}/add-dns-record")
    public ResponseEntity<DNSResourceRecord> addDnsRecord(
            @PathVariable("domain-name") String domainName,
            @RequestBody DNSResourceRecord record
    ) {
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/domain/process-sync")
    public ResponseEntity<Void> processDomainsSync(@RequestBody DomainSyncReport domainSyncReport) {
        try {
            //Синхронизируем существующие
            domainSyncReport.getParams().forEach((key, value) -> publisher.publishEvent(new RegSpecUpdateEvent(key, value)));

            try {
                if (!domainSyncReport.getProblemRegistrars().isEmpty() && !domainSyncReport.isOnlyUkrnames()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<p>");
                    domainSyncReport.getProblemRegistrars().forEach(item -> {
                        sb.append(item);
                        sb.append("<br>");
                    });
                    sb.append("</p>");
                    ServiceMessage serviceMessage = new ServiceMessage();
                    serviceMessage.addParam("subject", "[HMS] При синхронизации доменов обнаружены пустые Регистраторы");
                    serviceMessage.addParam("body", sb.toString());
                    pmFeignClient.sendNotificationToDevs(serviceMessage);
                }
            } catch (Exception e) {
                logger.error("[processDomainsSync] Ошибка при отправке сообщения в pm: " + e.toString());
            }

            //Удаляем reg-spec у необновляющихся более 4 часов.
            publisher.publishEvent(new DomainClearSyncEvent("DomainClearSyncEvent", domainSyncReport.getProblemRegistrars()));

            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/domain/{domain-name}/delete-dns-record")
    public ResponseEntity deleteDnsRecord(@PathVariable("domain-name") String domainName, @RequestBody DNSResourceRecord record) {
        try {
            governorOfDnsRecord.drop(record.getRecordId().toString());
            return new ResponseEntity(HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/domain/{domainId}")
    public Domain readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("domainId") String domainId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", domainId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/domain")
    public Collection<Domain> readAll() {
        return governor.buildAll();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/domain")
    public Collection<Domain> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/domain/filter")
    public Collection<Domain> readAllWithParams(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/domain/filter")
    public Collection<Domain> readAllWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/domain/find")
    public Domain readOneWithParams(@RequestParam Map<String, String> requestParams) {
        return governor.build(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/domain/find")
    public Domain readOneWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.build(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/domain/get-dns-record/{recordId}")
    public DNSResourceRecord readOneDnsRecord(@PathVariable String accountId, @PathVariable String recordId) {
        return governorOfDnsRecord.build(recordId);
    }
}

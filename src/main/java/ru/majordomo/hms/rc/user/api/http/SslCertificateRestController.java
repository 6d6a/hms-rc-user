package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SslCertificateRestController {

    private GovernorOfSSLCertificate governor;

    @Autowired
    public void setGovernor(GovernorOfSSLCertificate governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/ssl-certificate/{sslCertificateId}", "/ssl-certificate/{sslCertificateId}/"}, method = RequestMethod.GET)
    public SSLCertificate readOne(@PathVariable String sslCertificateId) {
        return governor.build(sslCertificateId);
    }

    @RequestMapping(value = {"{accountId}/ssl-certificate/{sslCertificateId}", "{accountId}/ssl-certificate/{sslCertificateId}/"}, method = RequestMethod.GET)
    public SSLCertificate readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("sslCertificateId") String sslCertificateId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", sslCertificateId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/ssl-certificate/","/ssl-certificate"}, method = RequestMethod.GET)
    public Collection<SSLCertificate> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/ssl-certificate", "/{accountId}/ssl-certificate/"}, method = RequestMethod.GET)
    public Collection<SSLCertificate> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }
}

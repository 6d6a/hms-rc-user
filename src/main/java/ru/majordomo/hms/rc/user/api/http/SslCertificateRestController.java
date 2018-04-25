package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/ssl-certificate/{sslCertificateId}")
    public SSLCertificate readOne(@PathVariable String sslCertificateId) {
        return governor.build(sslCertificateId);
    }

    @GetMapping("{accountId}/ssl-certificate/{sslCertificateId}")
    public SSLCertificate readOneByAccountId(
            @PathVariable("accountId") String accountId,
            @PathVariable("sslCertificateId") String sslCertificateId
    ) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", sslCertificateId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/ssl-certificate")
    public Collection<SSLCertificate> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/ssl-certificate")
    public Collection<SSLCertificate> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }
}

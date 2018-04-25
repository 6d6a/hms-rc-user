package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.managers.GovernorOfFTPUser;
import ru.majordomo.hms.rc.user.resources.FTPUser;

@RestController
public class FTPUserRestController {

    private GovernorOfFTPUser governor;

    @Autowired
    public void setGovernor(GovernorOfFTPUser governor) {
        this.governor = governor;
    }

    @GetMapping("/ftp-user/filter")
    public FTPUser filter(@RequestParam Map<String, String> requestParams) {
        return governor.build(requestParams);
    }

    @GetMapping("/{accountId}/ftp-user/filter")
    public FTPUser filter(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.build(requestParams);
    }

    @GetMapping("/ftp-user/{ftpUserId}")
    public FTPUser readOne(@PathVariable String ftpUserId) {
        return governor.build(ftpUserId);
    }

    @GetMapping("{accountId}/ftp-user/{ftpUserId}")
    public FTPUser readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("ftpUserId") String ftpUserId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", ftpUserId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/ftp-user")
    public Collection<FTPUser> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/ftp-user")
    public Collection<FTPUser> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @GetMapping("/{accountId}/ftp-user/count")
    public Count countByAccountId(@PathVariable String accountId) {
        return governor.countByAccountId(accountId);
    }

}

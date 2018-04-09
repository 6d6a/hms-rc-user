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

    @RequestMapping(value = "/ftp-user/filter", method = RequestMethod.GET)
    public FTPUser filter(@RequestParam Map<String, String> requestParams) {
        return governor.build(requestParams);
    }

    @RequestMapping(value = "/{accountId}/ftp-user/filter", method = RequestMethod.GET)
    public FTPUser filter(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.build(requestParams);
    }

    @RequestMapping(value = {"/ftp-user/{ftpUserId}", "/ftp-user/{ftpUserId}/"}, method = RequestMethod.GET)
    public FTPUser readOne(@PathVariable String ftpUserId) {
        return governor.build(ftpUserId);
    }

    @RequestMapping(value = {"{accountId}/ftp-user/{ftpUserId}", "{accountId}/ftp-user/{ftpUserId}/"}, method = RequestMethod.GET)
    public FTPUser readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("ftpUserId") String ftpUserId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", ftpUserId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/ftp-user/","/ftp-user"}, method = RequestMethod.GET)
    public Collection<FTPUser> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/ftp-user", "/{accountId}/ftp-user/"}, method = RequestMethod.GET)
    public Collection<FTPUser> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/{accountId}/ftp-user/count", "/{accountId}/ftp-user/count/"}, method = RequestMethod.GET)
    public Count countByAccountId(@PathVariable String accountId) {
        return governor.countByAccountId(accountId);
    }

}

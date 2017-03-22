package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.WebSite;

@RestController
public class WebSiteRESTController {

    private GovernorOfWebSite governor;

    @Autowired
    public void setGovernor(GovernorOfWebSite governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/website/{websiteId}", "/website/{websiteId}/"}, method = RequestMethod.GET)
    public WebSite readOne(@PathVariable String websiteId) {
        return (WebSite) governor.build(websiteId);
    }

    @RequestMapping(value = {"{accountId}/website/{websiteId}", "{accountId}/website/{websiteId}/"}, method = RequestMethod.GET)
    public WebSite readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("websiteId") String websiteId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", websiteId);
        keyValue.put("accountId", accountId);
        return (WebSite) governor.build(keyValue);
    }

    @RequestMapping(value = {"/website/", "/website"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/website", "/{accountId}/website/"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"/{accountId}/website/count", "/{accountId}/website/count/"}, method = RequestMethod.GET)
    public Count countByAccountId(@PathVariable String accountId) {
        return governor.countByAccountId(accountId);
    }

    @RequestMapping(value = "/{accountId}/website/find", method = RequestMethod.GET)
    public WebSite readOneWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return (WebSite) governor.build(requestParams);
    }

    @RequestMapping(value = "/website/find", method = RequestMethod.GET)
    public WebSite readOneWithParams(@RequestParam Map<String, String> requestParams) {
        return (WebSite) governor.build(requestParams);
    }
}

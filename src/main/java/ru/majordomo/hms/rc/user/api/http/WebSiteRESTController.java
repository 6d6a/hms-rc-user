package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resources.WebSite;

@RestController
public class WebSiteRESTController {

    private GovernorOfWebSite governor;

    @Autowired
    public void setGovernor(GovernorOfWebSite governor) {
        this.governor = governor;
    }

    @GetMapping("/website/{websiteId}")
    public WebSite readOne(@PathVariable String websiteId) {
        return governor.build(websiteId);
    }

    @GetMapping("{accountId}/website/{websiteId}")
    public WebSite readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("websiteId") String websiteId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", websiteId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/website")
    public Collection<WebSite> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/website")
    public Collection<WebSite> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @GetMapping("/{accountId}/website/count")
    public Count countByAccountId(@PathVariable String accountId) {
        return governor.countByAccountId(accountId);
    }

    @GetMapping("/{accountId}/website/find")
    public WebSite readOneWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.build(requestParams);
    }

    @GetMapping("/website/find")
    public WebSite readOneWithParams(@RequestParam Map<String, String> requestParams) {
        return governor.build(requestParams);
    }

    @GetMapping("/{accountId}/website/filter")
    public Collection<WebSite> filterByAccountId(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @GetMapping("/website/filter")
    public Collection<WebSite> filter(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }
}

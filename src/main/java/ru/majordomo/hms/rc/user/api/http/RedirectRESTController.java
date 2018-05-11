package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.managers.GovernorOfRedirect;
import ru.majordomo.hms.rc.user.resources.Redirect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RedirectRESTController {

    private GovernorOfRedirect governor;

    @Autowired
    public void setGovernor(GovernorOfRedirect governor) {
        this.governor = governor;
    }

    @GetMapping("/redirect/{redirectId}")
    public Redirect readOne(@PathVariable String redirectId) {
        return governor.build(redirectId);
    }

    @GetMapping("{accountId}/redirect/{redirectId}")
    public Redirect readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("redirectId") String redirectId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", redirectId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/redirect")
    public Collection<Redirect> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/redirect")
    public Collection<Redirect> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @GetMapping("/{accountId}/redirect/find")
    public Redirect readOneWithParamsByAccount(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.build(requestParams);
    }

    @GetMapping("/redirect/find")
    public Redirect readOneWithParams(@RequestParam Map<String, String> requestParams) {
        return governor.build(requestParams);
    }

    @GetMapping("/{accountId}/redirect/filter")
    public Collection<Redirect> filterByAccountId(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @GetMapping("/redirect/filter")
    public Collection<Redirect> filter(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }
}

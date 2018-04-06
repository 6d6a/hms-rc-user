package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.rc.user.managers.GovernorOfResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ResourceArchiveRestController {

    private GovernorOfResourceArchive governor;

    @Autowired
    public void setGovernor(GovernorOfResourceArchive governor) {
        this.governor = governor;
    }

    @GetMapping("/resource-archive/{resourceArchiveId}")
    public ResourceArchive readOne(@PathVariable String resourceArchiveId) {
        return governor.build(resourceArchiveId);
    }

    @GetMapping("{accountId}/resource-archive/{resourceArchiveId}")
    public ResourceArchive readOneByAccountId(
            @PathVariable("accountId") String accountId,
            @PathVariable("resourceArchiveId") String resourceArchiveId
    ) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceArchiveId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @GetMapping("/resource-archive")
    public Collection<ResourceArchive> readAll() {
        return governor.buildAll();
    }

    @GetMapping("/{accountId}/resource-archive")
    public Collection<ResourceArchive> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @GetMapping("/{accountId}/resource-archive/filter")
    public Collection<ResourceArchive> filterByAccountId(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @GetMapping("/resource-archive/filter")
    public Collection<ResourceArchive> filter(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }
}

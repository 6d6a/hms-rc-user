package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.rc.user.api.DTO.Count;
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/resource-archive/{resourceArchiveId}")
    public ResourceArchive readOne(@PathVariable String resourceArchiveId) {
        return governor.build(resourceArchiveId);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
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

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/resource-archive")
    public Collection<ResourceArchive> readAll() {
        return governor.buildAll();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/resource-archive")
    public Collection<ResourceArchive> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/resource-archive/filter")
    public Collection<ResourceArchive> filterByAccountId(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.buildAll(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/resource-archive/count")
    public Count countByAccountId(@PathVariable String accountId, @RequestParam Map<String, String> requestParams) {
        requestParams.put("accountId", accountId);
        return governor.count(requestParams);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/resource-archive/filter")
    public Collection<ResourceArchive> filter(@RequestParam Map<String, String> requestParams) {
        return governor.buildAll(requestParams);
    }
}

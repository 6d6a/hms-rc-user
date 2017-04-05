package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
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

    @RequestMapping(value = {"/resource-archive/{resourceArchiveId}", "/resource-archive/{resourceArchiveId}/"}, method = RequestMethod.GET)
    public ResourceArchive readOne(@PathVariable String resourceArchiveId) {
        return governor.build(resourceArchiveId);
    }

    @RequestMapping(value = {"{accountId}/resource-archive/{resourceArchiveId}", "{accountId}/resource-archive/{resourceArchiveId}/"}, method = RequestMethod.GET)
    public ResourceArchive readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("resourceArchiveId") String resourceArchiveId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceArchiveId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/resource-archive/","/resource-archive"}, method = RequestMethod.GET)
    public Collection<ResourceArchive> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/resource-archive", "/{accountId}/resource-archive/"}, method = RequestMethod.GET)
    public Collection<ResourceArchive> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }
}

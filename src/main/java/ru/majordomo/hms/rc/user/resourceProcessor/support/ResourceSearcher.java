package ru.majordomo.hms.rc.user.resourceProcessor.support;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.util.Map;

@FunctionalInterface
public interface ResourceSearcher<T extends Resource> {
    T build(Map<String, String> keyValue) throws ResourceNotFoundException;
}

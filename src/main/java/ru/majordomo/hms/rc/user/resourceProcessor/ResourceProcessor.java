package ru.majordomo.hms.rc.user.resourceProcessor;

import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resources.Resource;

@FunctionalInterface
public interface ResourceProcessor<T extends Resource> {
    void process(ResourceActionContext<T> context) throws Exception;
}

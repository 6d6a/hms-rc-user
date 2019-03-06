package ru.majordomo.hms.rc.user.resourceProcessor.support;

import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resources.Resource;

@FunctionalInterface
public interface RoutingKeyResolver<T extends Resource> {
    String get(ResourceActionContext<T> context);
}

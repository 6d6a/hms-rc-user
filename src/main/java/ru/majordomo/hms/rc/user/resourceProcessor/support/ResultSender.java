package ru.majordomo.hms.rc.user.resourceProcessor.support;

import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resources.Resource;

@FunctionalInterface
public interface ResultSender<T extends Resource> {
    void send(ResourceActionContext<T> context, String routingKey);
}

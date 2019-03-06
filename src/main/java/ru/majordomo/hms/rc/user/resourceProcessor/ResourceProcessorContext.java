package ru.majordomo.hms.rc.user.resourceProcessor;

import ru.majordomo.hms.rc.user.managers.LordOfResources;
import ru.majordomo.hms.rc.user.resourceProcessor.support.ResourceByUrlBuilder;
import ru.majordomo.hms.rc.user.resourceProcessor.support.ResultSender;
import ru.majordomo.hms.rc.user.resourceProcessor.support.RoutingKeyResolver;
import ru.majordomo.hms.rc.user.resources.Resource;

public interface ResourceProcessorContext<T extends Resource> {
    LordOfResources<T> getGovernor();
    ResultSender<T> getSender();
    RoutingKeyResolver<T> getRoutingKeyResolver();
    ResourceByUrlBuilder<T> getResourceByUrlBuilder();
}

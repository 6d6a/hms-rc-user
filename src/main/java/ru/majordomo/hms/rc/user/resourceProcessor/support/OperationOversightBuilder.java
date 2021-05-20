package ru.majordomo.hms.rc.user.resourceProcessor.support;

import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.util.Optional;

@FunctionalInterface
public interface OperationOversightBuilder<T extends Resource> {
    Optional<OperationOversight<T>> get(String id);
}

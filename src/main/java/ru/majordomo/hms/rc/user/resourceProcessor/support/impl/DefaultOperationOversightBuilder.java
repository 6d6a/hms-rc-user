package ru.majordomo.hms.rc.user.resourceProcessor.support.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.managers.LordOfResources;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.support.OperationOversightBuilder;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class DefaultOperationOversightBuilder<T extends Resource> implements OperationOversightBuilder<T> {
    private final LordOfResources<T> governor;

    @Override
    public Optional<OperationOversight<T>> get(String id) {
        return governor.getOperationOversight(id);
    }
}

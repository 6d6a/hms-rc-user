package ru.majordomo.hms.rc.user.common;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;

import javax.annotation.Nonnull;

public interface BuildResourceWithoutBuiltIn<T extends Resource> extends ResourceByIdBuilder<T> {
    @Override
    default T build(@Nonnull String resourceId) throws ResourceNotFoundException {
        return build(resourceId, false);
    }

    /**
     * @param withoutBuiltIn не загружать и не добавлять сущности от которых зависит ресурс.
     * @throws ResourceNotFoundException если нет самого ресурса или ресурсов от которых он зависит
     */
    T build(@Nonnull String resourceId, boolean withoutBuiltIn) throws ResourceNotFoundException;
}

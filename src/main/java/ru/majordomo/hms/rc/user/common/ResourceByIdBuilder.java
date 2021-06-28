package ru.majordomo.hms.rc.user.common;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;

@FunctionalInterface
public interface ResourceByIdBuilder<T extends Resource>  {
    /**
     * @throws ResourceNotFoundException если нет самого ресурса или ресурсов от которых он зависит
     */
    T build(String resourceId) throws ResourceNotFoundException;
}

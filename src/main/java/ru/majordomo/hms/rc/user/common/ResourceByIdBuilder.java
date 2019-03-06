package ru.majordomo.hms.rc.user.common;

import ru.majordomo.hms.rc.user.resources.Resource;

@FunctionalInterface
public interface ResourceByIdBuilder<T extends Resource>  {
    T build(String id) throws Exception;
}

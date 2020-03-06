package ru.majordomo.hms.rc.user.common;

import lombok.Data;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.resources.Resource;

import javax.annotation.Nullable;

@Data
public class ResourceActionContext<T extends Resource> {
    private final ServiceMessage message;
    private final ResourceAction action;
    @Nullable
    private T resource;
    /**
     * Уже обработанное имя отправителя, такое как te, pm и т.д
     */
    private String eventProvider;
}

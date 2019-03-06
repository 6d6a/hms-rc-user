package ru.majordomo.hms.rc.user.common;

import lombok.Data;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.resources.Resource;

@Data
public class ResourceActionContext<T extends Resource> {
    private final ServiceMessage message;
    private final ResourceAction action;
    private T resource;
    private String eventProvider;
}

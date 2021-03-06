package ru.majordomo.hms.rc.user.common;

import lombok.Data;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resources.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Data
public class ResourceActionContext<T extends Resource> {
    private final ServiceMessage message;
    private final ResourceAction action;

    /**
     * Поле оставлено для обратной совместимости с другими сервисами
     */
    @Nullable
    private T resource;

    /**
     * Уже обработанное имя отправителя, такое как te, pm и т.д
     */
    private String eventProvider;

    /**
     * Сообщения которые необходимо добавить к TE
     */
    @Nonnull
    private Map<String, Object> extendedActionParams = new HashMap<>();

    private String routingKey = "";

    @Nullable
    private OperationOversight<T> ovs;

    public void setOvs(OperationOversight<T> ovs) {
        this.ovs = ovs;
        this.resource = ovs.getResource();
    }
}

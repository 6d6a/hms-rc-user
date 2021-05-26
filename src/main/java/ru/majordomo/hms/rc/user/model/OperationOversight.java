package ru.majordomo.hms.rc.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bson.types.ObjectId;
import org.jongo.marshall.jackson.oid.MongoId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.time.LocalDateTime;

@Data
@Document
public class OperationOversight<T extends Resource> {
    /**
     * Используется для хранения текущей операции с ресурсом.
     * Пока из TE не придёт ответа с успехом или неудачей, изменений в соответствующей коллекции не будет.
     *
     * action и resourceClass используются ТОЛЬКО для определения таймаута после которого придёт сообщение в алерту инженерам
     */

    @Id
    @MongoId
    private String id;

    /**
     * Используется для поддержания консистентности, для одного ресурса может существовать только один
     * объект OperationOversight
     */
    @Indexed(unique = true, sparse = true)
    private String resourceId;

    @JsonIgnore
    private ResourceAction action;

    @JsonIgnore
    private String resourceClass;

    private T resource;

    /**
     * Замена ресурса при создании
     */
    private Boolean replace = false;

    @JsonIgnore
    private LocalDateTime created = LocalDateTime.now();

    public OperationOversight() {}

    public OperationOversight(T resource, ResourceAction action) {
        this.resource = resource;
        this.resourceId = resource.getId();
        this.action = action;
        this.resourceClass = resource.getClass().getName();
        genResId();
    }

    public OperationOversight(T resource, ResourceAction action, Boolean replace) {
        this.resource = resource;
        this.resourceId = resource.getId();
        this.action = action;
        this.resourceClass = resource.getClass().getName();
        this.replace = replace;
        genResId();
    }

    /**
     * Генерация ID для ресурса при операции на создание. Необходимо для TE.
     */
    private void genResId() {
        if (this.resource != null && this.resource.getId() == null) {
            String genId = new ObjectId().toString();
            this.resource.setId(genId);
            this.resourceId = genId;
        }
    }
}

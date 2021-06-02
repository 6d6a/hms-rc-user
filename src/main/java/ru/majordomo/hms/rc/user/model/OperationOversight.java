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
import java.util.ArrayList;
import java.util.List;

@Data
@Document
public class OperationOversight<T extends Resource> {
    /**
     * Используется для хранения текущей операции с ресурсом.
     * Пока из TE не придёт ответа с успехом или неудачей, изменений в соответствующей коллекции не будет.
     *
     * action и resourceClass используются ТОЛЬКО для определения таймаута после которого придёт сообщение в алерту инженерам
     *
     * affectedResources содержат в себе зависимости, которые изменяются после или во время изменения ресурса,
     * в TE отправляются уже заранее изменённые зависимости. (Изменения применяется на основную коллекцию только после ответа от TE)
     * (Исключение - логика SSL сертификатов)
     *
     * requiredResources содержат в себе зависимости необходимые только для TE (Изменений в них не происходит)
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
     * Зависимости которые изменяются в ходе изменения ресурса (изменения применяются после получения ответа от TE)
     */
    private List<? extends Resource> affectedResources = new ArrayList<>();

    /**
     * Зависимости от которых завист изменение ресурса
     */
    private List<? extends Resource> requiredResources = new ArrayList<>();

    /**
     * Замена ресурса при создании
     */
    private Boolean replace = false;

    @JsonIgnore
    private LocalDateTime created = LocalDateTime.now();

    public OperationOversight() {}

    public OperationOversight(T resource, ResourceAction action) {
        defaultConstruct(resource, action);
    }

    public OperationOversight(T resource, ResourceAction action, Boolean replace) {
        defaultConstruct(resource, action);
        this.replace = replace;
    }

    public OperationOversight(T resource, ResourceAction action, Boolean replace, List<? extends Resource> affectedResources, List<? extends Resource> requiredResources) {
        defaultConstruct(resource, action);
        this.replace = replace;
        this.affectedResources = affectedResources != null ? affectedResources : new ArrayList<>();
        this.requiredResources = requiredResources != null ? requiredResources : new ArrayList<>();
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

    private void defaultConstruct(T resource, ResourceAction action) {
        this.resource = resource;
        this.resourceId = resource.getId();
        this.action = action;
        this.resourceClass = resource.getClass().getName();
        genResId();
    }
}

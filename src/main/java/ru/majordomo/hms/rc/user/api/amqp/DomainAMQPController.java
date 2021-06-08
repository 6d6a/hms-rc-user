package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.Constants;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.DefaultCreatePmProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.DefaultDeletePmProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.DefaultUpdatePmProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.domain.DomainTeCreateProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.domain.DomainTeDeleteProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.domain.DomainTeUpdateProcessor;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Redirect;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.WebSite;

import java.util.List;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DOMAIN_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DOMAIN_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DOMAIN_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class DomainAMQPController extends BaseAMQPController<Domain> {

    /**
     * Отдельная имплементация процессоров
     * @see DomainTeCreateProcessor
     * @see DomainTeUpdateProcessor
     * @see DomainTeDeleteProcessor
     * для домена.
     * Т.к. мы произвели необходимые действия с доменом ещё до того как отправили сообщение с affected ресурсами в TE,
     * то ответ от TE не имеет для нас значения. Поэтому перед отправкой в PM переопределяем параметр success на true,
     * (Например чтобы деньги за успешно продлённые домены списались, хоть и в TE что-то сфейлилось)
     */
    @Override
    protected ResourceProcessor<Domain> getEventProcessor(ResourceActionContext<Domain> context) {
        switch (context.getEventProvider()) {
            case TE:

                switch (context.getAction()) {
                    case CREATE:
                        return new DomainTeCreateProcessor(this);
                    case UPDATE:
                        return new DomainTeUpdateProcessor(this);
                    case DELETE:
                        return new DomainTeDeleteProcessor(this);
                }
            case PM:
                switch (context.getAction()) {
                    case CREATE:
                        return new DefaultCreatePmProcessor<>(this);
                    case UPDATE:
                        return new DefaultUpdatePmProcessor<>(this);
                    case DELETE:
                        return new DefaultDeletePmProcessor<>(this);
                }
        }
        return null;
    }

    @Autowired
    public void setGovernor(GovernorOfDomain governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleCreate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DOMAIN_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    public String getResourceType() {
        return Constants.Exchanges.Resource.DOMAIN;
    }

    @Override
    protected String getRoutingKey(ResourceActionContext<Domain> context) {
        if (context.getEventProvider().equals(PM)) {
            return getTaskExecutorRoutingKey(context);
        } else if (context.getEventProvider().equals(TE)) {
            return getDefaultRoutingKey();
        } else {
            return getDefaultRoutingKey();
        }
    }

    private String getTaskExecutorRoutingKey(ResourceActionContext<Domain> context) throws ParameterValidationException {

        if (context.getOvs() == null) {
            return PM;
        }

        List<? extends Resource> resources = context.getOvs().getAffectedResources();

        if (resources.isEmpty()) {
            return PM;
        }

        //Для домена может быть либо редирект, либо вебсайт в аффектед
        String serverName = null;
        try {
            for (Resource item : resources) {
                if (item instanceof WebSite) {
                    WebSite webSite = (WebSite) item;
                    serverName = staffRcClient.getServerByServiceId(webSite.getServiceId()).getName();
                    break;
                }
                if (item instanceof Redirect) {
                    Redirect redirect = (Redirect) item;
                    serverName = staffRcClient.getServerByServiceId(redirect.getServiceId()).getName();
                    break;
                }
            }
            return TE + "." + serverName.split("\\.")[0];
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[getTaskExecutorRoutingKey for Domain] got exception: {}",
                    e.getMessage());
            throw new InternalApiException("Exception: " + e.getMessage());
        }
    }
}

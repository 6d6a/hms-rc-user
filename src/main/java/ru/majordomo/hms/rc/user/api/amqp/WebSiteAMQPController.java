package ru.majordomo.hms.rc.user.api.amqp;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.Constants;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.website.WebsiteUpdateFromPm;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.WEBSITE_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.WEBSITE_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.WEBSITE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Component
public class WebSiteAMQPController extends BaseAMQPController<WebSite> {

    @Autowired
    public void setGovernor(GovernorOfWebSite governor) {
        this.governor = governor;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_CREATE)
    public void handleCreateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleCreate(eventProvider, serviceMessage);
    }

    @Override
    protected ResourceProcessor<WebSite> getEventProcessor(ResourceActionContext<WebSite> context) {
        if (PM.equals(context.getEventProvider()) && ResourceAction.UPDATE == context.getAction()) {
            return new WebsiteUpdateFromPm(this, staffRcClient);
        } else {
            return super.getEventProcessor(context);
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_UPDATE)
    public void handleUpdateEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + WEBSITE_DELETE)
    public void handleDeleteEvent(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    public String getResourceType() {
        return Constants.Exchanges.Resource.WEBSITE;
    }

    @Override
    protected String getRoutingKey(ResourceActionContext<WebSite> context) {
        if (context.getEventProvider().equals(PM)) {
            return getTaskExecutorRoutingKey(context.getResource());
        } else if (context.getEventProvider().equals(TE)) {
            return getDefaultRoutingKey();
        } else {
            return getDefaultRoutingKey();
        }
    }

    @Override
    protected ServiceMessage createReportMessage(ResourceActionContext<WebSite> context) {
        ServiceMessage result = super.createReportMessage(context);
        if (StringUtils.startsWith(context.getRoutingKey(), TE + ".") &&
                MapUtils.isNotEmpty(context.getExtendedActionParams()) &&
                MapUtils.getBooleanValue(context.getMessage().getParams(), "success", true) &&
                MapUtils.getString(context.getMessage().getParams(), "errorMessage", "").isEmpty()
        ) {
            // нужно как-то отделить сообщения корректные сообщения на изменения от собщений от te, ошибок rc-user, чего-нибудь еще
            result.getParams().putAll(context.getExtendedActionParams());
        }
        return result;
    }
}

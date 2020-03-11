package ru.majordomo.hms.rc.user.api.amqp;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.Constants;
import ru.majordomo.hms.rc.user.common.ExtendedAction;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.website.WebsiteUpdateFromPm;
import ru.majordomo.hms.rc.user.resources.WebSite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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
        if (StringUtils.startsWith(context.getRoutingKey(), TE + ".") && MapUtils.isNotEmpty(context.getExtendedActionParams())) {
            result.getParams().putAll(context.getExtendedActionParams());
        }
        return result;
//        ServiceMessage message = context.getMessage();
//        String provider = context.getEventProvider();
//        if (
//                message.getParam("success") != null || "te".equals(provider) ||
//                !MapUtils.getString(message.getParams(), "errorMessage", "").isEmpty()
//        ) {
//            // нужно как-то отделить сообщения корректные сообщения на изменения от собщений от te, ошибок rc-user, чего-нибудь еще
//            return super.createReportMessage(context);
//        }
//
//        if (context.getExtendedAction() == null || context.getResource() == null) {
//            return super.createReportMessage(context);
//        } else {
//            ServiceMessage report = super.createReportMessage(context);
//            try {
//                report.getParams().putAll(teExtendedAction(context));
//            } catch (RuntimeException ex) {
//                if (context.isWasSettingLocked()) {
//                    ((GovernorOfWebSite) governor).setLocked(context.getResource().getId(), false);
//                }
//
//            }
//            return report;
//        }
    }

//    private Map<String, Object> teExtendedAction(ResourceActionContext<WebSite> context) {
//        ExtendedAction extAction = context.getExtendedAction();
//        WebSite webSite = context.getResource();
//        if (extAction == null || webSite == null) {
//            return Collections.emptyMap();
//        }
//        Service staffService = staffRcClient.getService(webSite.getServiceId());
//        if (staffService == null || !(staffService.getTemplate() instanceof ApplicationServer)) {
//            throw new ParameterValidationException("Некорректный тип сервиса");
//        }
//        if (!staffService.isSwitchedOn()) {
//            throw new ParameterValidationException("Операции с выключенным сервисом");
//        }
//        if (StringUtils.isEmpty(staffService.getAccountId()) || !staffService.getAccountId().equals(webSite.getAccountId())) {
//            throw new ParameterValidationException("Некорректный владелец сервиса");
//        }
//        ApplicationServer template = (ApplicationServer) staffService.getTemplate();
//        String deployImage = template.getDeployImagePath();
//        if (StringUtils.isEmpty(deployImage)) {
//            throw new ParameterValidationException("Для выбранного сервиса невозможна установка пользовательских приложений");
//        }
//
//        Map<String, Object> result;
//        switch (extAction) {
//            case LOAD:
//                if (!EnumSet.of(ResourceAction.UPDATE, ResourceAction.CREATE).contains(context.getAction())) {
//                    throw new ParameterValidationException("Действие возможно только для созданного сайта");
//                }
//                result = new HashMap<String, Object>() {{
//                    put("datasourceUri", webSite.getAppLoadUrl());
//                    put("dataSourceParams", webSite.getAppLoadParams());
//                }};
//                break;
//            case INSTALL:
//            case SHELL:
//                if (context.getAction() != ResourceAction.UPDATE) {
//                    throw new ParameterValidationException("Действие возможно только для созданного сайта");
//                }
//                List<String> command = ExtendedAction.INSTALL == extAction ? Collections.singletonList("install") :
//                        Arrays.asList("shell", StringUtils.trimToEmpty(webSite.getAppInstallCommands()));
//                result = new HashMap<String, Object>() {{
//                    put("dataPostprocessorType", "docker");
//                    put("dataPostprocessorArgs", new HashMap<String, Object>() {{
//                        put("image", deployImage);
//                        put("command", command);
//                    }});
//                }};
//                break;
//            default:
//                throw new ParameterValidationException("Для сайта запрошено неподдерживаемое действие");
//        }
//        return result;
//    }
}

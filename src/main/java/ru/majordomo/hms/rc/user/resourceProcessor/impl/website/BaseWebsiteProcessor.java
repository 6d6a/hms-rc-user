package ru.majordomo.hms.rc.user.resourceProcessor.impl.website;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;
import ru.majordomo.hms.rc.user.api.amqp.WebSiteAMQPController;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.common.ExtendedAction;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resources.WebSite;

import java.util.*;

@Slf4j
public abstract class BaseWebsiteProcessor implements ResourceProcessor<WebSite> {
    protected final WebSiteAMQPController processorContext;
    protected final StaffResourceControllerClient staffRcClient;

    public BaseWebsiteProcessor(WebSiteAMQPController processorContext, StaffResourceControllerClient staffRcClient) {
        this.processorContext = processorContext;
        this.staffRcClient = staffRcClient;
    }

    protected void validateAndFullExtendedAction(ResourceActionContext<WebSite> context) {
        String extendedActionRaw = MapUtils.getString(context.getMessage().getParams(), "extendedAction");
        ExtendedAction extendedAction;
        if (StringUtils.isNotEmpty(extendedActionRaw)) {
            try {
                extendedAction = ExtendedAction.valueOf(extendedActionRaw);
            } catch (IllegalArgumentException ex) {
                throw new ParameterValidationException("Некорректное дополнительное действие с ресурсом");
            }
        } else {
            return;
        }

        WebSite webSite = context.getResource();
        if (webSite == null) {
            return;
        }

        Service staffService = staffRcClient.getService(webSite.getServiceId());
        if (staffService == null || !(staffService.getTemplate() instanceof ApplicationServer)) {
            log.error("Attempt extended action for website: {} with wrong service: {}", webSite.getId(), staffService);
            throw new ParameterValidationException("Некорректный тип сервиса");
        }
        if (!staffService.isSwitchedOn()) {
            throw new ParameterValidationException("Операции с выключенным сервисом");
        }
        if (StringUtils.isEmpty(staffService.getAccountId()) || !staffService.getAccountId().equals(webSite.getAccountId())) {
            log.error("Attempt run extended action for website {}, for service {} with wrong owner", webSite.getId(), staffService.getId());
            throw new ParameterValidationException("Некорректный владелец сервиса");
        }
        ApplicationServer template = (ApplicationServer) staffService.getTemplate();
        String deployImage = template.getDeployImagePath();
        if (StringUtils.isEmpty(deployImage)) {
            log.error("Extended action {} for website {} is prohibited because don't set deployImagePath for templayte {}", extendedAction, webSite.getId(), template.getId());
            throw new ParameterValidationException("Для выбранного сервиса невозможна установка пользовательских приложений");
        }
        switch (extendedAction) {
            case LOAD:
                if (!EnumSet.of(ResourceAction.UPDATE, ResourceAction.CREATE).contains(context.getAction())) {
                    throw new ParameterValidationException("Действие возможно только при создании и изменении сайта");
                }
                if (StringUtils.isEmpty(webSite.getAppLoadUrl())) {
                    throw new ParameterValidationException("Для загрузки приложения необходимо задать адрес");
                }
                context.getExtendedActionParams().put("datasourceUri", webSite.getAppLoadUrl());
                context.getExtendedActionParams().put("dataSourceParams", webSite.getAppLoadParams());
                context.getExtendedActionParams().put("extendedAction", extendedAction);
                context.getExtendedActionParams().put("maxRetries", 0);
                break;

            case INSTALL:
                if (context.getAction() != ResourceAction.UPDATE) {
                    throw new ParameterValidationException("Действие возможно только для созданного сайта");
                }
                context.getExtendedActionParams().put("maxRetries", 0);
                context.getExtendedActionParams().put("dataPostprocessorType", "docker");
                context.getExtendedActionParams().put("dataPostprocessorArgs", new HashMap<String, Object>() {{
                    put("image", deployImage);
                    put("command",  Collections.singletonList("install"));
                }});
                context.getExtendedActionParams().put("extendedAction", extendedAction);
                break;

            case LOAD_INSTALL:
                if (!EnumSet.of(ResourceAction.UPDATE, ResourceAction.CREATE).contains(context.getAction())) {
                    throw new ParameterValidationException("Действие возможно только при создании и изменении сайта");
                }
                if (StringUtils.isEmpty(webSite.getAppLoadUrl())) {
                    throw new ParameterValidationException("Для загрузки приложения необходимо задать адрес");
                }
                context.getExtendedActionParams().putAll(new HashMap<String, Object>() {{
                    put("extendedAction", extendedAction);
                    put("maxRetries", 0);
                    put("dataPostprocessorType", "docker");
                    put("datasourceUri", webSite.getAppLoadUrl());
                    put("dataSourceParams", webSite.getAppLoadParams());
                    put("dataPostprocessorArgs", new HashMap<String, Object>() {{
                        put("image", deployImage);
                        put("command",  Collections.singletonList("install"));
                    }});
                }});
                break;

            case SHELL:
                if (context.getAction() != ResourceAction.UPDATE) {
                    throw new ParameterValidationException("Действие возможно только для созданного сайта");
                }
                if (StringUtils.isBlank(webSite.getAppInstallCommands())) {
                    throw new ParameterValidationException("Необходимо задать shell команды");
                }
                context.getExtendedActionParams().put("maxRetries", 0);
                context.getExtendedActionParams().put("dataPostprocessorType", "docker");
                context.getExtendedActionParams().put("dataPostprocessorArgs", new HashMap<String, Object>() {{
                    put("image", deployImage);
                    put("command",  Arrays.asList("shell", webSite.getAppInstallCommands()));
                }});
                context.getExtendedActionParams().put("extendedAction", extendedAction);
                break;

            case SHELLUPDATE:
                if (context.getAction() != ResourceAction.UPDATE) {
                    throw new ParameterValidationException("Действие возможно только для созданного сайта");
                }
                if (StringUtils.isBlank(webSite.getAppUpdateCommands())) {
                    throw new ParameterValidationException("Необходимо задать shell команды");
                }
                context.getExtendedActionParams().putAll(new HashMap<String, Object>() {{
                    put("maxRetries", 0);
                    put("dataPostprocessorType", "docker");
                    put("extendedAction", extendedAction);
                    put("dataPostprocessorArgs", new HashMap<String, Object>() {{
                        put("image", deployImage);
                        put("command",  Arrays.asList("shell", webSite.getAppUpdateCommands()));
                    }});
                }});
                break;
        }
    }
}

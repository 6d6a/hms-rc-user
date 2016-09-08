package ru.majordomo.hms.rc.user.api.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.WebSite;

@EnableRabbit
@Component
public class WebSiteAMQPController {

    private final static Logger logger = LoggerFactory.getLogger(WebSiteAMQPController.class);

    @Autowired
    GovernorOfWebSite governorOfWebSite;

    @Autowired
    Sender sender;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = "service.rc-user", durable = "true", autoDelete = "true"),
                    exchange = @Exchange(value = "website.create", type = "topic"),
                    key = "service.rc.user"),
                    containerFactory = "")
    public void create(@Payload ServiceMessage serviceMessage) {
        ServiceMessage reportServiceMessage = new ServiceMessage();
        reportServiceMessage.setOperationIdentity(serviceMessage.getOperationIdentity());
        reportServiceMessage.setActionIdentity(serviceMessage.getActionIdentity());
        String loggerPrefix = "OPERATION IDENTITY:" + serviceMessage.getOperationIdentity() + "ACTION IDENTITY:" + serviceMessage.getActionIdentity() + " ";

        try {
            WebSite webSite = (WebSite)governorOfWebSite.createResource(serviceMessage);
            reportServiceMessage.setObjRef("http://rc-user/website/"+webSite.getId());
            reportServiceMessage.addParam("success",Boolean.TRUE);
        } catch (ru.majordomo.hms.rc.user.exception.ParameterValidateException e) {
            logger.error(e.toString());
            reportServiceMessage.addParam("success",Boolean.FALSE);
        } finally {
            sender.send("website.create","service.pm",reportServiceMessage);
            logger.info(loggerPrefix+"Сообщение с отчетом отправлено " + reportServiceMessage.toString());
        }

    }
}

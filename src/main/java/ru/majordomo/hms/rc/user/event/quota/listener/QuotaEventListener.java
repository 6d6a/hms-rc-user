package ru.majordomo.hms.rc.user.event.quota.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.api.clients.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.event.quota.QuotaAlmostFullEvent;
import ru.majordomo.hms.rc.user.event.quota.QuotaAlreadyFullEvent;
import ru.majordomo.hms.rc.user.resources.Quotable;

@Component
public class QuotaEventListener {

    private final Sender sender;
    private String applicationName;

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Autowired
    public QuotaEventListener(
            Sender sender
    ){
        this.sender = sender;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onQuotaAlmostFullEvent(QuotaAlmostFullEvent event) {
        Quotable resource = event.getSource();

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("provider", applicationName);
        messageProperties.setContentType("application/json");

//        Message message = MessageBuilder
//                .withBody(message.toJson().getBytes())
//                .andProperties(messageProperties)
//                .build();
//
//        sender.send("mail.send", "pm", message);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onQuotaAlreadyFullEvent(QuotaAlreadyFullEvent event) {
        Quotable resource = event.getSource();
    }
}

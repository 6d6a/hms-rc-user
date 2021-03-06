package ru.majordomo.hms.rc.user.api.interfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.configurations.FeignConfig;


@FeignClient(name = "pm", configuration = FeignConfig.class)
public interface PmFeignClient {
    @PostMapping(value = "/notifications/send-to-client", consumes = "application/json;utf8")
    ServiceMessage sendNotificationToClient(
            @RequestBody ServiceMessage message
    );
    @PostMapping(value = "/notifications/send-to-dev", consumes = "application/json;utf8")
    ServiceMessage sendNotificationToDevs(
            @RequestBody ServiceMessage message
    );
    @PostMapping(value = "/phpmail-disable-notify")
    String sendPhpMailNotificationToClient(
            @RequestBody String accountId
    );
}


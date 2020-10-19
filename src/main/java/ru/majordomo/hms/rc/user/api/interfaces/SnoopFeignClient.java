package ru.majordomo.hms.rc.user.api.interfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.majordomo.hms.rc.user.api.DTO.log.FindFtpLogRequest;
import ru.majordomo.hms.rc.user.api.DTO.log.ScrollRequest;
import ru.majordomo.hms.rc.user.configurations.FeignSnoopConfig;

@FeignClient(configuration = FeignSnoopConfig.class, name = "snoop", url = "${snoop.url}", primary = false)
public interface SnoopFeignClient {
    @GetMapping("/search/ftp-log")
    String findFtpLog(@RequestParam String user, @RequestParam int page, @RequestParam int size);

    @PostMapping("/search/ftp-log/start")
    String findFtpLogStartScroll(
            @RequestBody FindFtpLogRequest request,
            @RequestParam int size
    );

    @PostMapping(value = "/search/ftp-log/continue", consumes = "application/json")
    String findFtpLogContinueScroll(@RequestBody ScrollRequest scrollRequest);

    @PostMapping(value = "/search/ftp-log/clear", consumes = "application/json")
    Void findFtpLogClearScroll(@RequestBody ScrollRequest scrollRequest);
}

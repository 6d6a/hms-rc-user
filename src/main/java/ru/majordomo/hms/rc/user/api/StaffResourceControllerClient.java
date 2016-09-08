package ru.majordomo.hms.rc.user.api;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import ru.majordomo.hms.rc.user.Resource;

@FeignClient(name = "rc-staff")
public interface StaffResourceControllerClient {
    @RequestMapping(method = RequestMethod.GET, value = "/hosting-server?state=")
    List<Resource> getActiveHostingServers();
}

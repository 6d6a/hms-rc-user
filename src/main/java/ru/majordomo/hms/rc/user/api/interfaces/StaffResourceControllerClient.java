package ru.majordomo.hms.rc.user.api.interfaces;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import ru.majordomo.hms.rc.staff.resources.Server;

@FeignClient("rc-staff")
public interface StaffResourceControllerClient {
    @RequestMapping(method = RequestMethod.GET, value = "/hosting-server?state=active", consumes = "application/json;utf8")
    Server getActiveHostingServers();

    @RequestMapping(method = RequestMethod.GET, value = "/db-server?state=active", consumes = "application/json;utf8")
    Server getActiveDatabaseServer();

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}", consumes = "application/json;utf8")
    Server getServerById(@PathVariable("serverId") String serverId);
}

package ru.majordomo.hms.rc.user.api.interfaces;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;

@FeignClient("rc-staff")
public interface StaffResourceControllerClient {
    @RequestMapping(method = RequestMethod.GET, value = "/server/filter?server-role=shared-hosting&state=active", consumes = "application/json;utf8")
    Server getActiveHostingServer();

    @RequestMapping(method = RequestMethod.GET, value = "/server/filter?server-role=database-server&state=active", consumes = "application/json;utf8")
    Server getActiveDatabaseServer();

    @RequestMapping(method = RequestMethod.GET, value = "/server/filter?server-role=mail-storage&state=active", consumes = "application/json;utf8")
    Server getActiveMailboxServer();

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}", consumes = "application/json;utf8")
    Server getServerById(@PathVariable("serverId") String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}/services?service-type=WEBSITE")
    List<Service> getWebsiteServicesByServerIdAndServiceType(@PathVariable("serverId") String serverId);
}

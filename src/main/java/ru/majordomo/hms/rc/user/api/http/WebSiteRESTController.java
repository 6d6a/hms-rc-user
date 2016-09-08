package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.WebSite;

@RestController
@CrossOrigin("*")
public class WebSiteRESTController {

    @Autowired
    WebSiteRepository webSiteRepository;

    @RequestMapping(value = "/rc/website/{websiteId}", method = RequestMethod.GET)
    public WebSite readOne(@PathVariable String websiteId) {
        return webSiteRepository.findOne(websiteId);
    }

    @RequestMapping(value = "/rc/website", method = RequestMethod.GET)
    public Collection<WebSite> readAll() {
        return webSiteRepository.findAll();
    }
}

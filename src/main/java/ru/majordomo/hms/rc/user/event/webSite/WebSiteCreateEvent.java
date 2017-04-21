package ru.majordomo.hms.rc.user.event.webSite;

import ru.majordomo.hms.rc.user.event.ResourceCreateEvent;
import ru.majordomo.hms.rc.user.resources.WebSite;

public class WebSiteCreateEvent extends ResourceCreateEvent<WebSite> {
    public WebSiteCreateEvent(WebSite source) {
        super(source);
    }
}

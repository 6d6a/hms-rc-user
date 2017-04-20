package ru.majordomo.hms.rc.user.event.webSite;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.WebSite;

public class WebSiteCreateEvent extends ApplicationEvent {
    public WebSiteCreateEvent(WebSite source) {
        super(source);
    }

    @Override
    public WebSite getSource() {
        return (WebSite) super.getSource();
    }
}

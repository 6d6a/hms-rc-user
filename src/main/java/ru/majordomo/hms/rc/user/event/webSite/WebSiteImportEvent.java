package ru.majordomo.hms.rc.user.event.webSite;

import org.springframework.context.ApplicationEvent;

public class WebSiteImportEvent extends ApplicationEvent {
    public WebSiteImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}

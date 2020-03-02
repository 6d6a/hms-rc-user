package ru.majordomo.hms.rc.user.event;


import org.springframework.context.ApplicationEvent;

public abstract class ResourceImportEvent extends ApplicationEvent {
    private String serverId;

    public ResourceImportEvent(String source, String serverId) {
        super(source);
        this.serverId = serverId;
    }

    public String getServerId() {
        return serverId;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}

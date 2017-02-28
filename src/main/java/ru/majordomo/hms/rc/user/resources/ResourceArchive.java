package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "resourceArchives")
public class ResourceArchive extends Resource implements Serviceable {
    private ResourceArchiveType resourceType;
    private String resourceId;
    @Transient
    private Resource resource;
    private String fileLink;
    private String serviceId;

    @CreatedDate
    private LocalDateTime createdAt;

    public ResourceArchiveType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceArchiveType resourceType) {
        this.resourceType = resourceType;
    }

    @JsonIgnore
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public void switchResource() {
        this.switchedOn = !this.switchedOn;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
}

package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

@Document(collection = "resourceArchives")
public class ResourceArchive extends Resource implements Serviceable {
    @NotNull(message = "Необходимо указать тип ресурса")
    private ResourceArchiveType resourceType;

    @NotNull(message = "Необходимо указать id ресурса")
    @Indexed
    private String archivedResourceId;

    @Transient
    private Resource resource;
    private String fileLink;

    @Indexed
    private String serviceId;

    @CreatedDate
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public ResourceArchiveType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceArchiveType resourceType) {
        this.resourceType = resourceType;
    }

    @JsonIgnore
    public String getArchivedResourceId() {
        return archivedResourceId;
    }

    public void setArchivedResourceId(String archivedResourceId) {
        this.archivedResourceId = archivedResourceId;
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

    public String getFileExtension() {
        return resourceType != null ? ResourceArchiveType.FILE_EXTENSION.get(resourceType) : ResourceArchiveType.DEFAULT_FILE_EXTENSION;
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

    @Override
    public String toString() {
        return "ResourceArchive{" +
                "resourceType=" + resourceType +
                ", archivedResourceId='" + archivedResourceId + '\'' +
                ", resource=" + resource +
                ", fileLink='" + fileLink + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", createdAt=" + createdAt +
                "} " + super.toString();
    }
}

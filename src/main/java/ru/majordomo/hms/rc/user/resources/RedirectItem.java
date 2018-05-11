package ru.majordomo.hms.rc.user.resources;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Data
public class RedirectItem {
    public enum Protocol { HTTP, HTTPS }

    @NotBlank
    private String targetUrl;

    @NotBlank
    private String sourcePath;

    @NotNull
    private Protocol targetProtocol;
}

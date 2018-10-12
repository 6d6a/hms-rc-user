package ru.majordomo.hms.rc.user.configurations;

import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;
import ru.majordomo.hms.rc.user.resources.CharSet;

import static ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings.SETTINGS_PREFIX;

@Data
@ConfigurationProperties(prefix = SETTINGS_PREFIX)
public class DefaultWebSiteSettings {
    public static final String SETTINGS_PREFIX = "default.website";

    @NotNull
    private final Boolean accessByOldHttpVersion;
    @NotNull
    private final Boolean accessLogEnabled;
    @NotNull
    private final Boolean allowUrlFopen;
    @NotNull
    private final Boolean autoSubDomain;
    @NotNull
    private final CharSet charset;
    @NotBlank
    private final String customUserConf;
    @NotNull
    private final Boolean ddosProtection;
    @NotBlank
    private final String documentRootPattern;
    @NotNull
    private final Boolean errorLogEnabled;
    @NotNull
    private final Boolean followSymLinks;
    @NotEmpty
    private final List<String> indexFileList;
    @NotNull
    @Range(min = 0, max = 7, message = "mbstringFuncOverload должно быть между {min} и {max}")
    private Integer mbstringFuncOverload;
    @NotNull
    private final Boolean multiViews;
    @NotBlank
    private final String scriptAlias;
    @NotBlank
    private final String serviceName;
    @NotEmpty
    private final List<String> fileExtensions;
    @NotNull
    private final FileExtensionsSettings cgi;
    @NotNull
    private final FileExtensionsSettings ssi;
    @NotEmpty
    private final List<String> staticFileExtensions;

    @Data
    private class FileExtensionsSettings {
        private final Boolean enabled = true;
        @NotEmpty
        private final List<String> fileExtensions;
    }
}

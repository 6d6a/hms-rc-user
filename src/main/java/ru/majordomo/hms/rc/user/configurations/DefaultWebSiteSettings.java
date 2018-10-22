package ru.majordomo.hms.rc.user.configurations;

import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;
import ru.majordomo.hms.rc.user.resources.CharSet;

import static ru.majordomo.hms.rc.user.configurations.DefaultWebSiteSettings.SETTINGS_PREFIX;

@Data
@ConfigurationProperties(prefix = SETTINGS_PREFIX)
@Validated
public class DefaultWebSiteSettings {
    public static final String SETTINGS_PREFIX = "default.website";

    @NotNull
    private Boolean accessByOldHttpVersion;
    @NotNull
    private Boolean accessLogEnabled;
    @NotNull
    private Boolean allowUrlFopen;
    @NotNull
    private Boolean autoSubDomain;
    @NotNull
    private CharSet charset;
    @NotNull
    private String customUserConf;
    @NotNull
    private Boolean ddosProtection;
    @NotBlank
    private String documentRootPattern;
    @NotNull
    private Boolean errorLogEnabled;
    @NotNull
    private Boolean followSymLinks;
    @NotEmpty
    private List<String> indexFileList;
    @NotNull
    @Range(min = 0, max = 7, message = "mbstringFuncOverload должно быть между {min} и {max}")
    private Integer mbstringFuncOverload;
    @NotNull
    private Boolean multiViews;
    @NotBlank
    private String scriptAlias;
    @NotBlank
    private String serviceName;
    @NotNull
    @NestedConfigurationProperty
    @Valid
    private FileExtensionsSettings cgi = new FileExtensionsSettings();
    @NotNull
    @NestedConfigurationProperty
    @Valid
    private FileExtensionsSettings ssi = new FileExtensionsSettings();
    @NotNull
    private FileExtensionsSettings Static = new FileExtensionsSettings();

    @Data
    public class FileExtensionsSettings {
        private Boolean enabled = true;
        @NotEmpty
        private List<String> fileExtensions;
    }
}

package ru.majordomo.hms.rc.user.resources;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class RedirectItem {
    @NotBlank(message = "Адрес назначения не может быть пустым")
    private String targetUrl;

    @NotNull(message = "Исходный адрес не может быть 'null'")
    private String sourcePath;
}

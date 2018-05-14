package ru.majordomo.hms.rc.user.resources;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class RedirectItem {
    @NotBlank(message = "Адрес назначения не может быть пустым")
    @URL(
            regexp = "^(http://|https://){1}",
            message = "Адрес назначения должен быть валидным URL по протоколу http или https")
    private String targetUrl;

    @NotNull(message = "Исходный адрес не может быть 'null'")
    @Pattern(regexp = "^(/\\S)*?$", message = "Некорректный исходный адрес, должен быть относительным")
    private String sourcePath;
}

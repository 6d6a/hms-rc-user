package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.user.resources.validation.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Document(collection = "dkim")
@NoArgsConstructor
@AllArgsConstructor
public class DKIM {
    /** для упрощения реализации это id домена */
    @Id
    @ObjectId(value = Domain.class)
    @NotBlank
    @JsonIgnore
    private String id;

    /**
     * приватный ключ которым подписываются письма в формате понимаемом нашим почтовым сервером. Одна большая строка вида:
     * -----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAnVjJ5X1M7WTNaAuLT294NPFu29msuJ3aj1xzsCbYyVfVxwSL\nnP ... lPquQ==\n-----END RSA PRIVATE KEY-----\n
     */
    @Nullable
    @NotBlank
    private String privateKey;

    /** публичный ключ в base64 */
    @NotBlank
    @Nullable
    private String publicKey;

    @Pattern(regexp = "^[a-zA-Z]{1,63}$", message = "Некорректный dkim селектор")
    private String selector;

    private boolean switchedOn;

    /**
     * Поле Data которое нужно установить DNS-записи DKIM и "значение" в GUI.
     * Содержащит приватный ключ и настройки для проверки подписи
     * Вычисляется при чтении из монго, на основе publicKey и шаблона в конфигурации spring
     */
    @Transient
    private String data;
}

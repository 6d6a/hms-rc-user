package ru.majordomo.hms.rc.user.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import ru.majordomo.hms.rc.user.resources.validation.UniqueNameResource;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "SSLCertificates")
@UniqueNameResource(SSLCertificate.class)
@Data
@EqualsAndHashCode(callSuper = true)
public class SSLCertificate extends Resource {
    private String key;

    private String csr;

    private String cert;

    private String chain;

    private Map<String, String> issuerInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime notAfter;

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }
}
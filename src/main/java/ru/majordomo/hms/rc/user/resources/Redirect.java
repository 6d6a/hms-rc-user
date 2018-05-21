package ru.majordomo.hms.rc.user.resources;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.rc.user.resources.validation.ObjectId;
import ru.majordomo.hms.rc.user.resources.validation.ServiceId;
import ru.majordomo.hms.rc.user.resources.validation.ValidRedirect;
import ru.majordomo.hms.rc.user.resources.validation.group.RedirectChecks;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
@ValidRedirect
public class Redirect extends Resource implements Serviceable {

    @ServiceId(groups = RedirectChecks.class)
    @Indexed
    private String serviceId;

    @ObjectId(value = Domain.class, message = "Домен с id ${validatedValue} не существует")
    @Indexed
    private String domainId;

    @Transient
    private Domain domain;

    @Valid
    private Set<RedirectItem> redirectItems = new HashSet<>();

    @Override
    public void switchResource() {
        switchedOn = !switchedOn;
    }
}
package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.service.stat.Aggregator;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/stat")
@Validated
public class StatRestController {
    private Aggregator aggregator;

    @Autowired
    public StatRestController(
            Aggregator aggregator
    ) {
        this.aggregator = aggregator;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/aggregate/{resource}")
    public Collection getUnixAccountStat(@PathVariable String resource) {
        try {
            switch (resource) {
                case "unix-account":
                    return aggregator.getStat(UnixAccount.class);
                case "database":
                    return aggregator.getStat(Database.class);
                case "mailbox":
                    return aggregator.getStat(Mailbox.class);
                case "database-user":
                    return aggregator.getStat(DatabaseUser.class);
                case "website":
                    return aggregator.getStat(WebSite.class);
                default:
                    throw new ParameterValidationException("Неизвестный тип ресурса");
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/account-id-and-field/{resource}/{fieldName}")
    public Map<String, String> getAccountIdAndServerId(
            @Valid @PathVariable @Pattern(regexp = "^(unix-account|mailbox)$") String resource,
            @Valid @PathVariable @Pattern(regexp = "^(serverId|serviceId)$") String fieldName
    ) {
        return aggregator.getAccountIdAndField(resource, fieldName);
    }
}

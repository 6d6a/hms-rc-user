package ru.majordomo.hms.rc.user.api.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.rc.user.importing.*;

/**
 * Контроллер для запуска команд на импорт из billingdb в hms
 * Импортируется только часть ресурсов которым не требуется te.
 * Другая часть импортируется на стороне pm
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ImportRestController {
    private final PersonDBImportService personDBImportService;
    private final DomainDBImportService domainDBImportService;
    private final DomainSubDomainDBImportService domainSubDomainDBImportService;
    private final MailboxDBImportService mailboxDBImportService;

    /**
     * импорт
     * @param accountId - id аккаунта в billingdb и personmgr
     * @param accountEnabled - включен ли аккаунт, нужно ли включать ресурсы
     * @param allowAntispam - на аккаунте есть услуга антиспам
     * @return - void
     */
    @PostMapping("/import/{accountId}")
    @PreAuthorize("principal.username == 'service'")
    public ResponseEntity<Void> importToMongo(
            @PathVariable String accountId,
            @RequestParam(required = false, defaultValue = "true") boolean accountEnabled,
            @RequestParam(required = false, defaultValue = "true") boolean allowAntispam
    ) {
        try {
            boolean imported;
            imported = personDBImportService.importToMongo(accountId, "");
            log.debug(imported ? "person db_imported" : "domain db_not_imported");
            imported = domainDBImportService.importToMongo(accountId, "");
            log.debug(imported ? "domain db_imported" : "domain db_not_imported");
            imported = domainSubDomainDBImportService.importToMongo(accountId, "");
            log.debug(imported ? "domainSubDomain db_imported" : "domainSubDomain db_not_imported");

            imported = mailboxDBImportService.importToMongo(accountId, accountEnabled, allowAntispam);
            log.debug(imported ? "mailbox db_imported" : "mailbox db_not_imported");

            return ResponseEntity.ok().build();
        } catch (BaseException ex) {
            log.error("Exception when import account: " + accountId, ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Exception when import account: " + accountId, ex);
            throw new InternalApiException(ex.toString());
        }
    }
}

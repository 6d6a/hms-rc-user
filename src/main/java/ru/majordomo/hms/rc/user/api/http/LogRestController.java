package ru.majordomo.hms.rc.user.api.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.DTO.log.FindFtpLogRequest;
import ru.majordomo.hms.rc.user.api.DTO.log.ScrollRequest;
import ru.majordomo.hms.rc.user.api.interfaces.SnoopFeignClient;
import ru.majordomo.hms.rc.user.repositories.FTPUserRepository;
import ru.majordomo.hms.rc.user.resources.FTPUser;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LogRestController {
    private final SnoopFeignClient snoopFeignClient;
    private final FTPUserRepository ftpUserRepository;

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping({"/{accountId}/find-logs/ftp/{ftpUserId}", "/{accountId}/ftp-user/log/{ftpUserId}"})
    public ResponseEntity<String> getFtpLog(
            @PathVariable("accountId") String accountId,
            @PathVariable("ftpUserId") String ftpUserId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        try {
            FTPUser ftpUser = ftpUserRepository.findByIdAndAccountId(ftpUserId, accountId);
            if (ftpUser == null) {
                throw new ResourceNotFoundException();
            }
            return ResponseEntity.ok(snoopFeignClient.findFtpLog(ftpUser.getName(), page, size));
        } catch (Exception e) {
            log.error("we got exception when snoop request", e);
            throw new InternalApiException();
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @PostMapping({"/{accountId}/find-logs/ftp/start/{ftpUserId}", "/{accountId}/ftp-user/find-logs/start/{ftpUserId}"})
    public ResponseEntity<String> findFtpLogStartScroll(
            @PathVariable("accountId") String accountId,
            @PathVariable("ftpUserId") String ftpUserId,
            @RequestBody FindFtpLogRequest findFtpLogRequest,
            @Min(1)@Max(1000)
            @RequestParam(value = "size", defaultValue = "50", required = false) int size
    ) {
        FTPUser ftpUser = ftpUserRepository.findByIdAndAccountId(ftpUserId, accountId);
        if (ftpUser == null) {
            throw new ResourceNotFoundException();
        }
        findFtpLogRequest.setUser(ftpUser.getName());
        return ResponseEntity.ok(snoopFeignClient.findFtpLogStartScroll(findFtpLogRequest, size));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @PostMapping({"/{accountId}/find-logs/ftp/continue", "/{accountId}/ftp-user/find-logs/continue"})
    public ResponseEntity<String> findFtpLogStartScroll(
            @PathVariable("accountId") String accountId,
            @Valid @RequestBody ScrollRequest scrollRequest) {
        try {
            return ResponseEntity.ok(snoopFeignClient.findFtpLogContinueScroll(scrollRequest));
        } catch (Exception e) {
            log.error("We got exception when continue ftp log", e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @PostMapping({"/{accountId}/find-logs/ftp/clear", "/{accountId}/ftp-user/find-logs/clear"})
    public ResponseEntity<Void> findFtpLogClearScroll(
            @PathVariable("accountId") String accountId,
            @Valid @RequestBody ScrollRequest scrollRequest
    ) {
        snoopFeignClient.findFtpLogClearScroll(scrollRequest);
        return ResponseEntity.ok().build();
    }
}

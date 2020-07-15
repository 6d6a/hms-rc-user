package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;

import javax.validation.ConstraintViolationException;

@RestController
public class PersonRestController {

    private GovernorOfPerson governor;

    @Autowired
    public void setGovernor(GovernorOfPerson governor) {
        this.governor = governor;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/person/{personId}")
    public Person readOne(@PathVariable String personId) {
        return governor.build(personId);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/person/{personId}")
    public Person readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("personId") String personId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", personId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping("/person")
    public Collection<Person> readAll() {
        return governor.buildAll();
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/person")
    public Collection<Person> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("{accountId}/person/{personId}/sync")
    public ResponseEntity syncPerson(@PathVariable("accountId") String accountId,@PathVariable("personId") String personId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", personId);
        keyValue.put("accountId", accountId);
        Person person = governor.build(keyValue);

        try {
            governor.manualSync(person);
        } catch (ConstraintViolationException e) {
            return new ResponseEntity<>("Ошибка при валидации персоны", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);

    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @PostMapping("/{accountId}/person")
    public Person addByNicHandle(
            @PathVariable String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        String nicHandle = requestBody.get("nicHandle");

        if (nicHandle == null || nicHandle.equals("")) {
            throw new ParameterValidationException("Для добавления персоны необходимо указать её nicHandle");
        }

        return governor.addByNicHandle(accountId, nicHandle);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or (hasRole('USER') and #accountId == principal.accountId)")
    @GetMapping("/{accountId}/person/find")
    public Collection<Person> find(@PathVariable String accountId, @RequestParam Map<String, String> keyValue) {
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @PatchMapping("/{accountId}/person/{personId}/lock")
    public ResponseEntity<Person> toggleLock(@PathVariable String accountId, @PathVariable String personId, @RequestBody Map<String, Boolean> params) {
        return ResponseEntity.ok(governor.setLocked(accountId, personId, params.get("lock")));
    }
}

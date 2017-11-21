package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Person;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

@RestController
public class PersonRestController {

    private GovernorOfPerson governor;

    @Autowired
    public void setGovernor(GovernorOfPerson governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/person/{personId}", "/person/{personId}/"}, method = RequestMethod.GET)
    public Person readOne(@PathVariable String personId) {
        return governor.build(personId);
    }

    @RequestMapping(value = {"{accountId}/person/{personId}", "{accountId}/person/{personId}/"}, method = RequestMethod.GET)
    public Person readOneByAccountId(@PathVariable("accountId") String accountId,@PathVariable("personId") String personId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", personId);
        keyValue.put("accountId", accountId);
        return governor.build(keyValue);
    }

    @RequestMapping(value = {"/person/","/person"}, method = RequestMethod.GET)
    public Collection<Person> readAll() {
        return governor.buildAll();
    }

    @RequestMapping(value = {"/{accountId}/person", "/{accountId}/person/"}, method = RequestMethod.GET)
    public Collection<Person> readAllByAccountId(@PathVariable String accountId) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        return governor.buildAll(keyValue);
    }

    @RequestMapping(value = {"{accountId}/person/{personId}/sync", "{accountId}/person/{personId}/sync/"}, method = RequestMethod.GET)
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

    @RequestMapping(value = "/{accountId}/person", method = RequestMethod.POST)
    public Person addByNicHandle(
            @PathVariable String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        String nicHandle = requestBody.get("nicHandle");

        if (nicHandle == null || nicHandle.equals("")) {
            throw new ParameterValidateException("Для добавления персоны необходимо указать её nicHandle");
        }

        return governor.addByNicHandle(accountId, nicHandle);
    }
}

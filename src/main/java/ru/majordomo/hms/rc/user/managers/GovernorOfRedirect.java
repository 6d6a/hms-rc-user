package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.api.DTO.Count;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.repositories.RedirectRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.validation.group.RedirectChecks;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.*;

import static ru.majordomo.hms.rc.user.common.Utils.mapContains;

@Component
public class GovernorOfRedirect extends LordOfResources<Redirect> {

    private RedirectRepository repository;
    private GovernorOfDomain governorOfDomain;
    private Cleaner cleaner;
    private Validator validator;
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setRepository(RedirectRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Redirect update(ServiceMessage serviceMessage) throws ParameterValidationException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Redirect redirect = build(keyValue);

        try {
            setParamsFromKeyValue(redirect, serviceMessage.getParams());
        } catch (ClassCastException e) {
            logger.error("Redirect update ClassCastException: " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(redirect);
        validate(redirect);
        store(redirect);

        return redirect;
    }

    private String stringValue(Map.Entry<Object, Object> entry) {
        return cleaner.cleanString((String) entry.getValue());
    }

    private Boolean booleanValue(Map.Entry<Object, Object> entry) {
        return cleaner.cleanBoolean(entry.getValue());
    }

    @Override
    public void preDelete(String resourceId) {}

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Ресурс не найден");
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    public Redirect buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Redirect redirect = new Redirect();

        setResourceParams(redirect, serviceMessage, cleaner);

        try {
            setParamsFromKeyValue(redirect, serviceMessage.getParams());
        } catch (ClassCastException e) {
            logger.error("Redirect buildResourceFromServiceMessage ClassCastException: " + e.getMessage() + " " + Arrays.toString(e.getStackTrace()));
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        return redirect;
    }

    @Override
    public void preValidate(Redirect redirect) {
        if ((redirect.getName() == null || redirect.getName().equals("")) && redirect.getDomain() != null) {
            redirect.setName(redirect.getDomain().getName());
        }

        if (redirect.getSwitchedOn() == null) {
            redirect.setSwitchedOn(true);
        }
    }

    @Override
    public void validate(Redirect redirect) throws ParameterValidationException {
        Set<ConstraintViolation<Redirect>> constraintViolations = validator.validate(redirect, RedirectChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("redirect: " + redirect + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(Redirect redirect) {
        throw new NotImplementedException();
    }

    @Override
    protected Redirect construct(Redirect redirect) throws ParameterValidationException {
        Domain domain = governorOfDomain.build(redirect.getDomainId());
        redirect.setDomain(domain);
        return redirect;
    }

    @Override
    public Redirect build(String resourceId) throws ResourceNotFoundException {
        Redirect redirect = repository.findOne(resourceId);
        if (redirect == null) {
            throw new ResourceNotFoundException("Редирект с ID " + resourceId + " не найден");
        }
        return construct(redirect);
    }

    @Override
    public Redirect build(Map<String, String> keyValue) throws ResourceNotFoundException {
        Redirect redirect = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            redirect = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        } else if (mapContains(keyValue, "domainId")) {
            if (mapContains(keyValue, "accountId")) {
                redirect = repository.findByDomainIdAndAccountId(keyValue.get("domainId"), keyValue.get("accountId"));
            } else {
                redirect = repository.findByDomainId(keyValue.get("domainId"));
            }
        }

        if (redirect == null) {
            throw new ResourceNotFoundException("Не найден редирект по параметрам: " + keyValue.toString());
        }

        return construct(redirect);
    }

    @Override
    public Collection<Redirect> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Redirect> buildedRedirects = new ArrayList<>();

        if (mapContains(keyValue, "accountId", "serviceId")) {
            for (Redirect redirect : repository.findByServiceIdAndAccountId(keyValue.get("serviceId"), keyValue.get("accountId"))) {
                buildedRedirects.add(construct(redirect));
            }
        } else if (mapContains(keyValue, "accountId")) {
            for (Redirect redirect : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedRedirects.add(construct(redirect));
            }
        } else if (mapContains(keyValue, "serviceId")) {
            for (Redirect redirect : repository.findByServiceId(keyValue.get("serviceId"))) {
                buildedRedirects.add(construct(redirect));
            }
        }

        return buildedRedirects;
    }

    @Override
    public Collection<Redirect> buildAll() {
        List<Redirect> buildedRedirects = new ArrayList<>();

        for (Redirect redirect : repository.findAll()) {
            buildedRedirects.add(construct(redirect));
        }

        return buildedRedirects;
    }

    @Override
    public void store(Redirect redirect) {
        repository.save(redirect);
    }

    public Count countByAccountIdAndDomainId(String accountId, String domainId) {
        Redirect redirect = repository.findByDomainIdAndAccountId(accountId, domainId);
        if (redirect == null) {
            return Count.zero();
        } else {
            Count count = new Count();
            count.setCount((long) redirect.getRedirectItems().size());
            return count;
        }
    }

    private void setParamsFromKeyValue(Redirect redirect, Map<Object, Object> keyValue) {
        for (Map.Entry<Object, Object> entry : keyValue.entrySet()) {
            switch (entry.getKey().toString()) {
                case "redirectItems":
                    redirect.setRedirectItems((Set<RedirectItem>) entry.getValue());

                    break;
                case "name":
                    redirect.setName(stringValue(entry));

                    break;
                case "domainId":
                    redirect.setDomainId(stringValue(entry));
                    Domain domain = governorOfDomain.build(redirect.getDomainId());
                    redirect.setDomain(domain);

                    break;
                case "serviceId":
                    redirect.setServiceId(stringValue(entry));

                    break;
                case "switchedOn":
                    redirect.setSwitchedOn(booleanValue(entry));

                    break;
                default:
                    break;
            }
        }
    }
}

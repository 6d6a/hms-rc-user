package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

public class GovernorOfDnsRecord extends LordOfResources {

    private Cleaner cleaner;
    private GovernorOfDomain governorOfDomain;

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException, UnsupportedEncodingException {
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {

    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException {
        return null;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {

    }

    @Override
    protected Resource construct(Resource resource) throws ParameterValidateException {
        return null;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return null;
    }

    @Override
    public void store(Resource resource) {

    }
}

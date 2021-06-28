package ru.majordomo.hms.rc.user.managers;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.common.ResourceByIdBuilder;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.OperationOversightRepository;
import ru.majordomo.hms.rc.user.resourceProcessor.support.ResourceSearcher;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.Serviceable;

import javax.annotation.Nonnull;

@AllArgsConstructor
public abstract class LordOfResources<T extends Resource> implements ResourceSearcher<T>, ResourceByIdBuilder<T> {
    private final OperationOversightRepository<T> operationOversightRepository;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Создание ресурса используя Oversight
     */
    public OperationOversight<T> createByOversight(ServiceMessage serviceMessage) throws ParameterValidationException {
        OperationOversight<T> ovs;

        try {
            T resource = buildResourceFromServiceMessage(serviceMessage);
            preValidate(resource);
            Boolean replace = Boolean.TRUE.equals(serviceMessage.getParam("replaceOldResource"));
            validate(resource);
            postValidate(resource);
            ovs = sendToOversight(resource, ResourceAction.CREATE, replace);
        } catch (ClassCastException | UnsupportedEncodingException e) {
            throw new ParameterValidationException("Один из параметров указан неверно:" + e.getMessage());
        }

        return ovs;
    }

    /**
     * Обновление ресурса используя Oversight
     */
    public abstract OperationOversight<T> updateByOversight(ServiceMessage serviceMessage) throws ParameterValidationException, UnsupportedEncodingException;

    public abstract void preDelete(String resourceId);

    public abstract void drop(String resourceId) throws ResourceNotFoundException;

    public abstract OperationOversight<T> dropByOversight(String resourceId) throws ResourceNotFoundException;

    public abstract T buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException, UnsupportedEncodingException;

    public abstract void validate(T resource) throws ParameterValidationException;

    public void validateImported(T resource) {}

    public void preValidate(T resource) {}

    protected void removeOldResource(T resource) { }

    public void postValidate(T resource) {}

    /**
     * в обход Oversight
     * Только для SSLCertificate
     */
    public void validateAndStore(T resource) {
        preValidate(resource);
        validate(resource);
        postValidate(resource);
        store(resource);
    }

    /**
     * в обход Oversight
     * Только для Импортов
     */
    public void validateAndStoreImported(T resource) {
        preValidate(resource);
        validateImported(resource);
        postValidate(resource);
        store(resource);
    }

    public void syncWithRedis(@Nonnull T resource) {}

    protected abstract T construct(T resource) throws ParameterValidationException;

    @Override
    public abstract T build(Map<String, String> keyValue) throws ResourceNotFoundException;

    public abstract Collection<T> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException;

    public abstract Collection<T> buildAll();

    public abstract void store(T resource);

    /**
     * Создание объекта Oversight в коллекции.
     * Пока Oversight существует для ресурса - другие операции невозможны
     */
    public OperationOversight<T> sendToOversight(T resource, ResourceAction action) {
        OperationOversight<T> ovs = new OperationOversight<>(resource, action);
        return operationOversightRepository.save(ovs);
    }

    public OperationOversight<T> sendToOversight(T resource, ResourceAction action, Boolean replace) {
        OperationOversight<T> ovs = new OperationOversight<>(resource, action, replace);
        return operationOversightRepository.save(ovs);
    }

    public OperationOversight<T> sendToOversight(T resource, ResourceAction action, Boolean replace, List<? extends Resource> affectedResources, List<? extends Resource> requiredResources) {
        OperationOversight<T> ovs = new OperationOversight<>(resource, action, replace, affectedResources, requiredResources);
        return operationOversightRepository.save(ovs);
    }

    public Optional<OperationOversight<T>> getOperationOversight(String id) {
        return operationOversightRepository.findByOvsId(id);
    }

    public Optional<OperationOversight<T>> getOperationOversightByResource(T resource) {
        return operationOversightRepository.findByResourceId(resource.getId());
    }

    /**
     * Создание/Изменение ресурса и последущие удаление Oversight
     */
    public T completeOversightAndStore(OperationOversight<T> ovs) {
        if (ovs.getReplace()) {
            removeOldResource(ovs.getResource());
        }
        T res = ovs.getResource();
        store(res);
        removeOversight(ovs);

        return res;
    }

    /**
     * Удаление ресурса, а также выполение preDelete внутри реализаций и последущие удаление Oversight
     */
    public void completeOversightAndDelete(OperationOversight<T> ovs) {
        drop(ovs.getResource().getId());

        removeOversight(ovs);
    }

    public void removeOversight(OperationOversight<T> ovs) {
        operationOversightRepository.delete(ovs);
    }

    protected Boolean hasResourceIdAndAccountId(Map<String, String> keyValue) {

        boolean byAccountId = false;
        boolean byId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("resourceId")) {
                byId = true;
            }
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        return (byAccountId && byId);
    }

    protected Boolean hasNameAndAccountId(Map<String, String> keyValue) {

        boolean byAccountId = false;
        boolean byName = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("name")) {
                byName = true;
            }
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        return (byAccountId && byName);
    }

    public void setResourceParams(
            T resource,
            ServiceMessage serviceMessage,
            Cleaner cleaner
    ) throws ClassCastException {
        String id = cleaner.cleanString((String) serviceMessage.getParam("id"));
        String accountId = cleaner.cleanString(serviceMessage.getAccountId());
        String name = cleaner.cleanString((String) serviceMessage.getParam("name"));

        if (id != null && !id.equals("")) {
            resource.setId(id);
        }

        if (!(accountId == null || accountId.equals(""))) {
            resource.setAccountId(accountId);
        }

        Object switchedOn = serviceMessage.getParam("switchedOn");
        if (switchedOn != null) {
            resource.setSwitchedOn((Boolean) switchedOn);
        }

        resource.setName(name);
    }

    void preValidateDatabaseServiceId(Serviceable serviceable, StaffResourceControllerClient staffRcClient, String defaultServiceName) {
        if (serviceable.getServiceId() == null || (serviceable.getServiceId().equals(""))) {
            String serverId = staffRcClient.getActiveDatabaseServer().getId();
            List<Service> databaseServices = staffRcClient.getDatabaseServicesByServerId(serverId);
            if (databaseServices != null) {
                for (Service service : databaseServices) {
                    if (service.getServiceTemplate().getServiceType().getName().equals(defaultServiceName)) {
                        serviceable.setServiceId(service.getId());
                        break;
                    }
                }
                if (serviceable.getServiceId() == null || (serviceable.getServiceId().equals(""))) {
                    log.error("Не найдено serviceType: " + defaultServiceName
                            + " для сервера: " + serverId);
                }
            }
        }

    }
}

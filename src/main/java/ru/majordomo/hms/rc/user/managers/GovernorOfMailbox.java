package ru.majordomo.hms.rc.user.managers;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

@Service
public class GovernorOfMailbox extends LordOfResources {
    private final String redisPrefix = "mailbox";
    private MailboxRepository repository;
    private GovernorOfDomain governorOfDomain;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setRepository(MailboxRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public RedisTemplate<String, String> redisTemplate;

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        Mailbox mailbox;
        try {
            mailbox = (Mailbox) buildResourceFromServiceMessage(serviceMessage);
            validate(mailbox);
            store(mailbox);
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }
        return mailbox;
    }

    @Override
    public Resource update(ServiceMessage serviceMessage)
            throws ParameterValidateException, UnsupportedEncodingException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Mailbox mailbox = (Mailbox) build(keyValue);

        try {
            for (Map.Entry<Object, Object> entry : serviceMessage.getParams().entrySet()) {
                switch (entry.getKey().toString()) {
                    case "password":
                        mailbox.setPasswordHashByPlainPassword(cleaner.cleanString((String) entry.getValue()));
                        break;
                    case "whiteList":
                        mailbox.setWhiteList(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "blackList":
                        mailbox.setBlackList(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "redirectAddresses":
                        mailbox.setRedirectAddresses(cleaner.cleanListWithStrings((List<String>) entry.getValue()));
                        break;
                    case "quota":
                        mailbox.setQuota(((Number) entry.getValue()).longValue());
                        break;
                    case "switchedOn":
                        mailbox.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        validate(mailbox);
        store(mailbox);

        return mailbox;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Не найден почтовый ящик с ID: " + resourceId);
        }
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage)
            throws ClassCastException {
        Mailbox mailbox = new Mailbox();
        LordOfResources.setResourceParams(mailbox, serviceMessage, cleaner);
        String plainPassword = null;
        List<String> redirectAddresses = new ArrayList<>();
        List<String> blackList = new ArrayList<>();
        List<String> whilteList = new ArrayList<>();
        Long quota = null;
        String domainId;

        try {
            if (serviceMessage.getParam("domainId") == null) {
                throw new ParameterValidateException("Не указан domainId");
            }
            domainId = cleaner.cleanString((String) serviceMessage.getParam("domainId"));

            if (serviceMessage.getParam("password") != null) {
                plainPassword = (String) serviceMessage.getParam("password");
            }

            if (serviceMessage.getParam("redirectAddresses") != null) {
                redirectAddresses = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("redirectAddresses"));
            }

            if (serviceMessage.getParam("blackList") != null) {
                blackList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("blackList"));
            }

            if (serviceMessage.getParam("whiteList") != null) {
                whilteList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("whiteList"));
            }

            if (serviceMessage.getParam("quota") != null) {
                quota = ((Number) serviceMessage.getParam("quota")).longValue();
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        Boolean antiSpamEnabled = (Boolean) serviceMessage.getParam("antiSpamEnabled");

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", domainId);
        keyValue.put("accountId", mailbox.getAccountId());
        mailbox.setDomain((Domain) governorOfDomain.build(keyValue));

        if (!hasUniqueAddress(mailbox)) {
            throw new ParameterValidateException("Почтовый ящик уже существует");
        }

        try {
            mailbox.setPasswordHashByPlainPassword(plainPassword);
        } catch (UnsupportedEncodingException e) {
            throw new ParameterValidateException("Недопустимые символы в пароле");
        }

        String serverId = findMailStorageServer(domainId);

        mailbox.setBlackList(blackList);
        mailbox.setWhiteList(whilteList);
        mailbox.setRedirectAddresses(redirectAddresses);
        mailbox.setQuota(quota);
        mailbox.setQuotaUsed(0L);
        mailbox.setWritable(true);
        mailbox.setServerId(serverId);
        mailbox.setAntiSpamEnabled(antiSpamEnabled);

        return mailbox;
    }

    private Boolean hasUniqueAddress(Mailbox mailbox) {
        return (repository.findByNameAndDomainId(mailbox.getName(), mailbox.getDomainId()) == null);
    }

    private String findMailStorageServer(String domainId) {
        List<Mailbox> mailboxes = repository.findByDomainId(domainId);
        String serverId;

        if (mailboxes != null && !mailboxes.equals(Collections.emptyList())) {
            serverId = mailboxes.get(0).getServerId();
        } else {
            try {
                serverId = staffRcClient.getActiveMailboxServer().getId();
            } catch (FeignException e) {
                throw new ParameterValidateException("Внутренняя ошибка");
            }
        }

        return serverId;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        Mailbox mailbox = (Mailbox) resource;
        if (mailbox.getName() == null || mailbox.getName().equals("")) {
            throw new ParameterValidateException("Имя ящика не может быть пустым");
        }

        if (mailbox.getPasswordHash() == null || mailbox.getPasswordHash().equals("")) {
            throw new ParameterValidateException("Не указан пароль для почтового ящика");
        }

        if (mailbox.getDomain() == null) {
            throw new ParameterValidateException("Для ящика должен быть указан домен");
        }

        if (mailbox.getQuota() == null) {
            mailbox.setQuota(0L);
        }

        if (mailbox.getQuotaUsed() == null) {
            mailbox.setQuotaUsed(0L);
        }

        if (mailbox.getQuota() < 0L) {
            throw new ParameterValidateException("Квота не может иметь отрицательное значение");
        }

        if (mailbox.getQuotaUsed() < 0L) {
            throw new ParameterValidateException("Использованная квота не может иметь отрицательное значение");
        }
    }

    @Override
    public Resource construct(Resource resource) throws ParameterValidateException {
        Mailbox mailbox = (Mailbox) resource;
        Domain domain = (Domain) governorOfDomain.build(mailbox.getDomainId());
        mailbox.setDomain(domain);
        return mailbox;
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Mailbox mailbox = repository.findOne(resourceId);
        if (mailbox == null) {
            throw new ResourceNotFoundException("Mailbox с ID:" + mailbox.getId() + " не найден");
        }
        return construct(mailbox);
    }

    @Override
    public Resource build(Map<String, String> keyValue) throws ResourceNotFoundException {
        Mailbox mailbox = null;

        if (hasResourceIdAndAccountId(keyValue)) {
            mailbox = repository.findByIdAndAccountId(keyValue.get("resourceId"), keyValue.get("accountId"));
        }

        if (mailbox == null) {
            throw new ResourceNotFoundException("Почтовый ящик не найден");
        }

        return construct(mailbox);
    }

    @Override
    public Collection<? extends Resource> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Mailbox> buildedMailboxes = new ArrayList<>();

        boolean byAccountId = false;

        for (Map.Entry<String, String> entry : keyValue.entrySet()) {
            if (entry.getKey().equals("accountId")) {
                byAccountId = true;
            }
        }

        if (byAccountId) {
            for (Mailbox mailbox : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedMailboxes.add((Mailbox) construct(mailbox));
            }
        }

        return buildedMailboxes;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<Mailbox> mailboxes = new ArrayList<>();
        for (Mailbox mailbox: repository.findAll()) {
            mailboxes.add((Mailbox) construct(mailbox));
        }
        return mailboxes;
    }

    @Override
    public void store(Resource resource) {
        Mailbox mailbox = (Mailbox) resource;
        repository.save(mailbox);
    }

    private class MailboxRedisKeys {
        String mailStorageKey;
        final String aliasesKey = "domainList:aliases";
        final String mailCheckerKey = "domainList:mailChecker";
        String passwordHashKey;
        String blackListKey;
        String whiteListKey;
        String redirectAddressesKey;

        MailboxRedisKeys(Mailbox mailbox) {
            String name = mailbox.getFullName();
            passwordHashKey = String.format("%s:%s:passwordHash", redisPrefix, name);
            blackListKey = String.format("%s:%s:blackList", redisPrefix, name);
            whiteListKey = String.format("%s:%s:whiteList", redisPrefix, name);
            redirectAddressesKey = String.format("%s:%s:redirectAddresses", redisPrefix, name);
            mailStorageKey = String.format("domainList:mailStorage:%s", staffRcClient.getServerById(mailbox.getServerId()).getName());
        }
    }

    private void processMailboxRedisActions(Mailbox mailbox, MailboxRedisKeys keys) {
        redisTemplate.boundValueOps(keys.passwordHashKey).set(mailbox.getPasswordHash());

        redisTemplate.delete(keys.blackListKey);
        for (String entry : mailbox.getBlackList()) {
            redisTemplate.boundSetOps(keys.blackListKey).add(entry);
        }

        redisTemplate.delete(keys.whiteListKey);
        for (String entry : mailbox.getWhiteList()) {
            redisTemplate.boundSetOps(keys.whiteListKey).add(entry);
        }

        redisTemplate.delete(keys.redirectAddressesKey);
        for (String entry : mailbox.getRedirectAddresses()) {
            redisTemplate.boundSetOps(keys.redirectAddressesKey).add(entry);
        }
    }

    private void processDomainListsActions(Mailbox mailbox, MailboxRedisKeys keys) {
        SetOperations<String, String> ops = redisTemplate.opsForSet();
        String domainName = governorOfDomain.build(mailbox.getDomainId()).getName();

        if (!ops.isMember(keys.mailStorageKey, domainName)) {
            ops.add(keys.mailStorageKey, domainName);
        }
        if (!ops.isMember(keys.aliasesKey, domainName) && mailbox.getRedirectAddresses().size() > 0) {
            ops.add(keys.aliasesKey, domainName);
        }
        if (!ops.isMember(keys.mailCheckerKey, domainName) && mailbox.getAntiSpamEnabled()) {
            ops.add(keys.mailCheckerKey, domainName);
        }
    }

    public void addToRedis(Mailbox mailbox) {
        MailboxRedisKeys keys = new MailboxRedisKeys(mailbox);

        processMailboxRedisActions(mailbox, keys);
        processDomainListsActions(mailbox, keys);
    }

}

package ru.majordomo.hms.rc.user.managers;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Storage;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.MailboxRedisRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;

@Service
public class GovernorOfMailbox extends LordOfResources {
    private MailboxRepository repository;
    private MailboxRedisRepository redisRepository;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;

    private SpamFilterAction defaultSpamFilterAction;
    private SpamFilterMood defaultSpamFilterMood;

    @Value("${default.mailbox.spamfilter.action}")
    public void setDefaultSpamFilterAction(SpamFilterAction spamFilterAction) {
        this.defaultSpamFilterAction = spamFilterAction;
    }

    @Value("${default.mailbox.spamfilter.mood}")
    public void setDefaultSpamFilterMood(SpamFilterMood spamFilterMood) {
        this.defaultSpamFilterMood = spamFilterMood;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setGovernorOfUnixAccount(GovernorOfUnixAccount governorOfUnixAccount) {
        this.governorOfUnixAccount = governorOfUnixAccount;
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
    public void setRedisRepository(MailboxRedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        Mailbox mailbox;
        try {
            mailbox = (Mailbox) buildResourceFromServiceMessage(serviceMessage);
            validate(mailbox);
            store(mailbox);
            syncWithRedis(mailbox);
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
                    case "antiSpamEnabled":
                        mailbox.setAntiSpamEnabled((Boolean) entry.getValue());
                        break;
                    case "switchedOn":
                        mailbox.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    case "isAggregator":
                        Boolean userValue = (Boolean) entry.getValue();
                        if (userValue) {
                            assignAsAggregator(mailbox);
                        } else {
                            mailbox.setIsAggregator(userValue);
                        }
                        break;
                    case "spamFilterAction":
                        String spamFilterActionAsString = cleaner.cleanString((String) serviceMessage.getParam("spamFilterAction"));
                        try {
                            mailbox.setSpamFilterAction(Enum.valueOf(SpamFilterAction.class, spamFilterActionAsString));
                        } catch (IllegalArgumentException e) {
                            throw new ParameterValidateException("Недопустимый тип действия");
                        }
                        break;
                    case "spamFilterMood":
                        String spamFilterMoodAsString = cleaner.cleanString((String) serviceMessage.getParam("spamFilterMood"));
                        try {
                            mailbox.setSpamFilterMood(Enum.valueOf(SpamFilterMood.class, spamFilterMoodAsString));
                        } catch (IllegalArgumentException e) {
                            throw new ParameterValidateException("Недопустимый тип придирчивости");
                        }
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

        if (mailbox.getIsAggregator() != null && mailbox.getIsAggregator()) {
            setAggregatorInRedis(mailbox);
        } else {
            dropAggregatorInRedis(mailbox);
        }
        syncWithRedis(mailbox);

        return mailbox;
    }

    private Mailbox assignAsAggregator(Mailbox mailbox) {
        Mailbox currentAggregator = repository.findByDomainIdAndIsAggregator(mailbox.getDomainId(), true);
        if (currentAggregator != null) {
            currentAggregator.setIsAggregator(false);
            store(currentAggregator);
        }
        mailbox.setIsAggregator(true);
        return mailbox;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Не найден почтовый ящик с ID: " + resourceId);
        }
        Mailbox mailbox = (Mailbox) build(resourceId);
        dropFromRedis(mailbox);
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
        List<String> whiteList = new ArrayList<>();
        Long quota = null;
        Boolean antiSpamEnabled = false;
        SpamFilterMood spamFilterMood = null;
        SpamFilterAction spamFilterAction = null;
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
                whiteList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("whiteList"));
            }

            if (serviceMessage.getParam("quota") != null) {
                quota = ((Number) serviceMessage.getParam("quota")).longValue();
            }

            if (serviceMessage.getParam("antiSpamEnabled") != null) {
                antiSpamEnabled = (Boolean) serviceMessage.getParam("antiSpamEnabled");
            }

            if (serviceMessage.getParam("spamFilterAction") != null) {
                String asString = cleaner.cleanString((String) serviceMessage.getParam("spamFilterAction"));
                try {
                    spamFilterAction = Enum.valueOf(SpamFilterAction.class, asString);
                } catch (IllegalArgumentException e) {
                    throw new ParameterValidateException("Недопустимый тип действия");
                }
            }

            if (serviceMessage.getParam("spamFilterMood") != null) {
                String asString = cleaner.cleanString((String) serviceMessage.getParam("spamFilterMood"));
                try {
                    spamFilterMood = Enum.valueOf(SpamFilterMood.class, asString);
                } catch (IllegalArgumentException e) {
                    throw new ParameterValidateException("Недопустимый тип придирчивости");
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", mailbox.getAccountId());

        List<UnixAccount> unixAccounts = (List<UnixAccount>) governorOfUnixAccount.buildAll(keyValue);
        if (unixAccounts.equals(Collections.emptyList())) {
            throw new ParameterValidateException("Не найдено UnixAccount для AccountID: " + mailbox.getAccountId());
        }

        Integer uid = unixAccounts.get(0).getUid();

        keyValue.put("resourceId", domainId);
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

        String mailSpool = null;
        if (serverId != null && !serverId.equals("")) {
            Storage storage = staffRcClient.getActiveMailboxStorageByServerId(serverId);
            if (storage != null) {
                mailSpool = storage.getMountPoint();
            }
        }

        if (mailSpool == null) {
            throw new ParameterValidateException("Внутренняя ошибка: не удалось сформировать");
        }

        mailbox.setBlackList(blackList);
        mailbox.setWhiteList(whiteList);
        mailbox.setRedirectAddresses(redirectAddresses);
        mailbox.setQuota(quota);
        mailbox.setQuotaUsed(0L);
        mailbox.setWritable(true);
        mailbox.setServerId(serverId);
        mailbox.setUid(uid);
        mailbox.setMailSpool(mailSpool);
        mailbox.setAntiSpamEnabled(antiSpamEnabled);
        mailbox.setSpamFilterAction(spamFilterAction);
        mailbox.setSpamFilterMood(spamFilterMood);

        return mailbox;
    }

    private Boolean hasAggregator(String domainId) {
        return repository.findByDomainIdAndIsAggregator(domainId, true) != null;
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
                throw new ParameterValidateException("Внутренняя ошибка: не удалось найти подходящий сервер");
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

        if (mailbox.getSpamFilterAction() == null) {
            mailbox.setSpamFilterAction(defaultSpamFilterAction);
        }

        if (mailbox.getSpamFilterMood() == null) {
            mailbox.setSpamFilterMood(defaultSpamFilterMood);
        }

        if (mailbox.getQuota() == null) {
            mailbox.setQuota(250000L);
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

    public MailboxForRedis convertMailboxToMailboxForRedis(Mailbox mailbox) {
        MailboxForRedis mailboxForRedis = new MailboxForRedis();
        mailboxForRedis.setName(mailbox.getFullName());
        mailboxForRedis.setPasswordHash(mailbox.getPasswordHash());
        mailboxForRedis.setBlackList(String.join(":", mailbox.getBlackList()));
        mailboxForRedis.setWhiteList(String.join(":", mailbox.getWhiteList()));
        mailboxForRedis.setRedirectAddresses(String.join(":", mailbox.getRedirectAddresses()));
        mailboxForRedis.setWritable(mailbox.getWritable());
        mailboxForRedis.setAntiSpamEnabled(mailbox.getAntiSpamEnabled());
        mailboxForRedis.setSpamFilterAction(mailbox.getSpamFilterAction());
        mailboxForRedis.setSpamFilterMood(mailbox.getSpamFilterMood());
        String serverName = staffRcClient.getServerById(mailbox.getServerId()).getName();
        mailboxForRedis.setServerName(serverName);

        return mailboxForRedis;
    }

    public void syncWithRedis(Mailbox mailbox) {
        redisRepository.save(convertMailboxToMailboxForRedis(mailbox));
    }

    public void dropFromRedis(Mailbox mailbox) {
        redisRepository.delete(mailbox.getFullName());
    }

    public void setAggregatorInRedis(Mailbox mailbox) {
        MailboxForRedis mailboxForRedis = new MailboxForRedis();
        mailboxForRedis.setName("*@" + ((Mailbox)construct(mailbox)).getDomain().getName());
        mailboxForRedis.setPasswordHash(mailbox.getPasswordHash());
        mailboxForRedis.setBlackList(String.join(":", mailbox.getBlackList()));
        mailboxForRedis.setWhiteList(String.join(":", mailbox.getWhiteList()));
        mailboxForRedis.setRedirectAddresses(String.join(":", mailbox.getRedirectAddresses()));
        mailboxForRedis.setWritable(mailbox.getWritable());
        mailboxForRedis.setAntiSpamEnabled(mailbox.getAntiSpamEnabled());
        mailboxForRedis.setSpamFilterAction(mailbox.getSpamFilterAction());
        mailboxForRedis.setSpamFilterMood(mailbox.getSpamFilterMood());
        String serverName = staffRcClient.getServerById(mailbox.getServerId()).getName();
        mailboxForRedis.setServerName(serverName);

        redisRepository.save(mailboxForRedis);
    }

    public void dropAggregatorInRedis(Mailbox mailbox) {
        redisRepository.delete("*@" + ((Mailbox)construct(mailbox)).getDomain().getName());
    }

}

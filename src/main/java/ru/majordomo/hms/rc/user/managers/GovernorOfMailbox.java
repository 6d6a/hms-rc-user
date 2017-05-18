package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.util.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

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
import ru.majordomo.hms.rc.user.resources.validation.group.MailboxChecks;

@Service
public class GovernorOfMailbox extends LordOfResources<Mailbox> {
    private MailboxRepository repository;
    private MailboxRedisRepository redisRepository;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;

    private SpamFilterAction defaultSpamFilterAction;
    private SpamFilterMood defaultSpamFilterMood;

    private Validator validator;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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

    @Autowired
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Mailbox create(ServiceMessage serviceMessage) throws ParameterValidateException {
        Mailbox mailbox;
        try {
            mailbox = buildResourceFromServiceMessage(serviceMessage);
            validateAndStore(mailbox);
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }
        return mailbox;
    }

    @Override
    public void validateAndStore(Mailbox mailbox) {
        preValidate(mailbox);
        validate(mailbox);
        store(mailbox);
        syncWithRedis(mailbox);
    }

    @Override
    public Mailbox update(ServiceMessage serviceMessage)
            throws ParameterValidateException, UnsupportedEncodingException {
        String resourceId = null;

        if (serviceMessage.getParam("resourceId") != null) {
            resourceId = (String) serviceMessage.getParam("resourceId");
        }

        String accountId = serviceMessage.getAccountId();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("resourceId", resourceId);
        keyValue.put("accountId", accountId);

        Mailbox mailbox = build(keyValue);

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
                    case "mailFromAllowed":
                        mailbox.setMailFromAllowed((Boolean) entry.getValue());
                        break;
                    case "antiSpamEnabled":
                        mailbox.setAntiSpamEnabled((Boolean) entry.getValue());
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
                    case "writable":
                        mailbox.setWritable((Boolean) entry.getValue());
                        break;
                    case "switchedOn":
                        Boolean switchedOn = (Boolean) entry.getValue();
                        mailbox.setSwitchedOn(switchedOn);
                        mailbox.setWritable(switchedOn);
                        mailbox.setMailFromAllowed(switchedOn);
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        validateAndStore(mailbox);

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
    public void preDelete(String resourceId) {
        Mailbox mailbox = build(resourceId);
        dropFromRedis(mailbox);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (repository.findOne(resourceId) == null) {
            throw new ResourceNotFoundException("Не найден почтовый ящик с ID: " + resourceId);
        }

        preDelete(resourceId);
        repository.delete(resourceId);
    }

    @Override
    protected Mailbox buildResourceFromServiceMessage(ServiceMessage serviceMessage)
            throws ClassCastException {
        Mailbox mailbox = new Mailbox();
        setResourceParams(mailbox, serviceMessage, cleaner);
        String plainPassword = null;
        List<String> redirectAddresses = new ArrayList<>();
        List<String> blackList = new ArrayList<>();
        List<String> whiteList = new ArrayList<>();
        Long quota = null;
        Boolean mailFromAllowed = true;
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

            if (serviceMessage.getParam("mailFromAllowed") != null) {
                mailFromAllowed = (Boolean) serviceMessage.getParam("mailFromAllowed");
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
                    throw new ParameterValidateException("Недопустимый тип придирчивости SPAM-фильтра");
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно");
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", mailbox.getAccountId());

        List<UnixAccount> unixAccounts = (List<UnixAccount>) governorOfUnixAccount.buildAll(keyValue);
        if (unixAccounts.isEmpty()) {
            throw new ParameterValidateException("Не найдено UnixAccount для AccountID: " + mailbox.getAccountId());
        }

        Integer uid = unixAccounts.get(0).getUid();

        keyValue.put("resourceId", domainId);
        mailbox.setDomain(governorOfDomain.build(keyValue));

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
            throw new ParameterValidateException("Внутренняя ошибка: не удалось сформировать mailSpool");
        }

        mailSpool = mailSpool + "/" + governorOfDomain.build(domainId).getName();

        mailbox.setBlackList(blackList);
        mailbox.setWhiteList(whiteList);
        mailbox.setRedirectAddresses(redirectAddresses);
        mailbox.setQuota(quota);
        mailbox.setQuotaUsed(0L);
        mailbox.setWritable(true);
        mailbox.setServerId(serverId);
        mailbox.setUid(uid);
        mailbox.setMailSpool(mailSpool);
        mailbox.setMailFromAllowed(mailFromAllowed);
        mailbox.setAntiSpamEnabled(antiSpamEnabled);
        mailbox.setSpamFilterAction(spamFilterAction);
        mailbox.setSpamFilterMood(spamFilterMood);

        return mailbox;
    }

    private Boolean hasAggregator(String domainId) {
        return repository.findByDomainIdAndIsAggregator(domainId, true) != null;
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
    public void preValidate(Mailbox mailbox) {
        if (mailbox.getQuota() == null) {
            mailbox.setQuota(250000L);
        }

        if (mailbox.getSpamFilterAction() == null) {
            mailbox.setSpamFilterAction(defaultSpamFilterAction);
        }

        if (mailbox.getSpamFilterMood() == null) {
            mailbox.setSpamFilterMood(defaultSpamFilterMood);
        }
    }

    @Override
    public void validate(Mailbox mailbox) throws ParameterValidateException {
        Set<ConstraintViolation<Mailbox>> constraintViolations = validator.validate(mailbox, MailboxChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("mailbox: " + mailbox + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(Mailbox mailbox) {
        Set<ConstraintViolation<Mailbox>> constraintViolations = validator.validate(mailbox, MailboxChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("[validateImported] mailbox: " + mailbox + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public Mailbox construct(Mailbox mailbox) throws ParameterValidateException {
        Domain domain = governorOfDomain.build(mailbox.getDomainId());
        mailbox.setDomain(domain);
        return mailbox;
    }

    @Override
    public Mailbox build(String resourceId) throws ResourceNotFoundException {
        Mailbox mailbox = repository.findOne(resourceId);
        if (mailbox == null) {
            throw new ResourceNotFoundException("Mailbox с ID:" + resourceId + " не найден");
        }
        return construct(mailbox);
    }

    @Override
    public Mailbox build(Map<String, String> keyValue) throws ResourceNotFoundException {
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
    public Collection<Mailbox> buildAll(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Mailbox> buildedMailboxes = new ArrayList<>();

        if (keyValue.get("accountId") != null) {
            for (Mailbox mailbox : repository.findByAccountId(keyValue.get("accountId"))) {
                buildedMailboxes.add(construct(mailbox));
            }
        } else if (keyValue.get("serverId") != null) {
            for (Mailbox mailbox : repository.findByServerId(keyValue.get("serverId"))) {
                buildedMailboxes.add(construct(mailbox));
            }
        } else if (keyValue.get("domainId") != null) {
            for (Mailbox mailbox : repository.findByDomainId(keyValue.get("domainId"))) {
                buildedMailboxes.add(construct(mailbox));
            }
        }

        return buildedMailboxes;
    }

    @Override
    public Collection<Mailbox> buildAll() {
        List<Mailbox> mailboxes = new ArrayList<>();
        for (Mailbox mailbox: repository.findAll()) {
            mailboxes.add(construct(mailbox));
        }
        return mailboxes;
    }

    @Override
    public void store(Mailbox mailbox) {
        repository.save(mailbox);
    }

    private MailboxForRedis convertMailboxToMailboxForRedis(Mailbox mailbox, String serverName) {
        MailboxForRedis mailboxForRedis = new MailboxForRedis();
        String uidAsString = mailbox.getUid().toString();
        mailboxForRedis.setId(mailbox.getFullNameInPunycode());
        mailboxForRedis.setName(mailbox.getFullNameInPunycode());
        mailboxForRedis.setPasswordHash(mailbox.getPasswordHash());
        mailboxForRedis.setBlackList(String.join(":", mailbox.getBlackListInPunycode()));
        mailboxForRedis.setWhiteList(String.join(":", mailbox.getWhiteListInPunycode()));
        mailboxForRedis.setRedirectAddresses(String.join(":", mailbox.getRedirectAddressesInPunycode()));
        mailboxForRedis.setWritable(mailbox.getWritable());
        mailboxForRedis.setMailFromAllowed(mailbox.getMailFromAllowed());
        mailboxForRedis.setAntiSpamEnabled(mailbox.getAntiSpamEnabled());
        mailboxForRedis.setSpamFilterAction(mailbox.getSpamFilterAction());
        mailboxForRedis.setSpamFilterMood(mailbox.getSpamFilterMood());
        mailboxForRedis.setServerName(serverName);
        mailboxForRedis.setStorageData(uidAsString + ":" + uidAsString + ":" + mailbox.getMailSpoolInPunycode());

        return mailboxForRedis;
    }

    private void saveUserData(Mailbox mailbox, String serverName) {
        String uidAsString = mailbox.getUid().toString();
        Map<String, String> userData = new HashMap<>();
        userData.put("uid", uidAsString);
        userData.put("gid", uidAsString);
        userData.put("mail", "maildir:" + mailbox.getMailSpoolInPunycode() + "/" + mailbox.getName());
        userData.put("home", mailbox.getMailSpoolInPunycode() + "/" + mailbox.getName());
        userData.put("host", serverName);
        userData.put("proxy_maybe", "y");
        userData.put("password", mailbox.getPasswordHash());
        ObjectMapper mapper = new ObjectMapper();
        String data = "";
        try {
            data = mapper.writeValueAsString(userData);
        } catch (JsonProcessingException e) {
            logger.error("Mailbox userData не записана в Redis!");
        }
        String key = "mailboxUserData:" + mailbox.getFullName();
        redisTemplate.boundValueOps(key).set(data);
    }

    public void syncWithRedis(Mailbox mailbox) {
        if (mailbox.getIsAggregator() != null && mailbox.getIsAggregator()) {
            dropAggregatorInRedis(mailbox);
            setAggregatorInRedis(mailbox);
        }

        String serverName = staffRcClient.getServerById(mailbox.getServerId()).getName();
        redisRepository.save(convertMailboxToMailboxForRedis(mailbox, serverName));
        saveUserData(mailbox, serverName);
    }

    private void dropFromRedis(Mailbox mailbox) {
        redisRepository.delete(mailbox.getFullName());
        String key = "mailboxUserData:" + mailbox.getFullName();
        redisTemplate.delete(key);
    }

    private void setAggregatorInRedis(Mailbox mailbox) {
        String uidAsString = mailbox.getUid().toString();
        MailboxForRedis mailboxForRedis = new MailboxForRedis();
        mailboxForRedis.setId("*@" + IDN.toASCII((construct(mailbox)).getDomain().getName()));
        mailboxForRedis.setName(mailbox.getFullNameInPunycode());
        mailboxForRedis.setPasswordHash(mailbox.getPasswordHash());
        mailboxForRedis.setBlackList(String.join(":", mailbox.getBlackListInPunycode()));
        mailboxForRedis.setWhiteList(String.join(":", mailbox.getWhiteListInPunycode()));
        mailboxForRedis.setRedirectAddresses(String.join(":", mailbox.getRedirectAddressesInPunycode()));
        mailboxForRedis.setWritable(mailbox.getWritable());
        mailboxForRedis.setMailFromAllowed(mailbox.getMailFromAllowed());
        mailboxForRedis.setAntiSpamEnabled(mailbox.getAntiSpamEnabled());
        mailboxForRedis.setSpamFilterAction(mailbox.getSpamFilterAction());
        mailboxForRedis.setSpamFilterMood(mailbox.getSpamFilterMood());
        String serverName = staffRcClient.getServerById(mailbox.getServerId()).getName();
        mailboxForRedis.setServerName(serverName);
        mailboxForRedis.setStorageData(uidAsString + ":" + uidAsString + ":" + mailbox.getMailSpoolInPunycode());

        redisRepository.save(mailboxForRedis);
    }

    private void dropAggregatorInRedis(Mailbox mailbox) {
        redisRepository.delete("*@" + (construct(mailbox)).getDomain().getName());
    }

    public void updateQuota(String mailboxId, Long quotaSize) {
        Mailbox mailbox = repository.findOne(mailboxId);
        if (mailbox != null) {
            mailbox.setQuotaUsed(quotaSize);
            if (mailbox.getQuotaUsed() > mailbox.getQuota()) {
                mailbox.setWritable(false);
            } else {
                mailbox.setWritable(true);
            }
        } else {
            throw new ResourceNotFoundException("Mailbox с ID: " + mailboxId + " не найден");
        }
        store(mailbox);
    }

}

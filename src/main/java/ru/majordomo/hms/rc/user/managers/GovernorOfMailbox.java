package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import com.mongodb.MongoClient;

import feign.FeignException;

import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.rc.staff.resources.Storage;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.event.quota.MailboxQuotaFullEvent;
import ru.majordomo.hms.rc.user.event.quota.MailboxQuotaWarnEvent;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.repositories.MailboxRedisRepository;
import ru.majordomo.hms.rc.user.repositories.MailboxRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.validation.group.MailboxChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.MailboxImportChecks;

@Service
public class GovernorOfMailbox extends LordOfResources<Mailbox> {
    private MailboxRepository repository;
    private MailboxRedisRepository redisRepository;
    private UnixAccountRepository unixAccountRepository;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;

    private SpamFilterAction defaultSpamFilterAction;
    private SpamFilterMood defaultSpamFilterMood;

    private Validator validator;

    private MongoClient mongoClient;
    private String springDataMongodbDatabase;
    private ApplicationEventPublisher publisher;
    private int warnPercent;

    @Value("${resources.quotable.warnPercent.mailbox}")
    public void setWarnPercent(int warnPercent){
        this.warnPercent = warnPercent;
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

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

    @Value("${spring.data.mongodb.database}")
    public void setSpringDataMongodbDatabase(String springDataMongodbDatabase) {
        this.springDataMongodbDatabase = springDataMongodbDatabase;
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
    public void setUnixAccountRepository(UnixAccountRepository unixAccountRepository) {
        this.unixAccountRepository = unixAccountRepository;
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

    @Autowired
    public void setMongoClient(@Qualifier("jongoMongoClient") MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public Mailbox create(ServiceMessage serviceMessage) throws ParameterValidationException {
        Mailbox mailbox;
        try {
            mailbox = buildResourceFromServiceMessage(serviceMessage);
            validateAndStore(mailbox);
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно:" + e.getMessage());
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
            throws ParameterValidationException, UnsupportedEncodingException {
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
                    case "comment":
                        mailbox.setComment(cleaner.cleanString((String) entry.getValue()));
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
                            if (mailbox.getIsAggregator() != null && mailbox.getIsAggregator()) {
                                dropAggregatorInRedis(mailbox);
                            }
                            mailbox.setIsAggregator(userValue);
                        }
                        break;
                    case "spamFilterAction":
                        String spamFilterActionAsString = cleaner.cleanString((String) serviceMessage.getParam("spamFilterAction"));
                        try {
                            mailbox.setSpamFilterAction(Enum.valueOf(SpamFilterAction.class, spamFilterActionAsString));
                        } catch (IllegalArgumentException e) {
                            throw new ParameterValidationException("Недопустимый тип действия");
                        }
                        break;
                    case "spamFilterMood":
                        String spamFilterMoodAsString = cleaner.cleanString((String) serviceMessage.getParam("spamFilterMood"));
                        try {
                            mailbox.setSpamFilterMood(Enum.valueOf(SpamFilterMood.class, spamFilterMoodAsString));
                        } catch (IllegalArgumentException e) {
                            throw new ParameterValidationException("Недопустимый тип придирчивости");
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
                    case "willBeDeletedAfter":
                        if (entry.getValue() == null) {
                            mailbox.setWillBeDeletedAfter(null);
                        } else {
                            mailbox.setWillBeDeletedAfter(LocalDateTime.parse((String) entry.getValue()));
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
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
    public Mailbox buildResourceFromServiceMessage(ServiceMessage serviceMessage)
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
        String comment = null;

        try {
            if (serviceMessage.getParam("domainId") == null) {
                throw new ParameterValidationException("Не указан domainId");
            }

            domainId = cleaner.cleanString((String) serviceMessage.getParam("domainId"));

            if (serviceMessage.getParam("password") != null) {
                plainPassword = (String) serviceMessage.getParam("password");
            }

            if (serviceMessage.getParam("comment") != null) {
                comment = (String) serviceMessage.getParam("comment");
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
                    throw new ParameterValidationException("Недопустимый тип действия");
                }
            }

            if (serviceMessage.getParam("spamFilterMood") != null) {
                String asString = cleaner.cleanString((String) serviceMessage.getParam("spamFilterMood"));
                try {
                    spamFilterMood = Enum.valueOf(SpamFilterMood.class, asString);
                } catch (IllegalArgumentException e) {
                    throw new ParameterValidationException("Недопустимый тип придирчивости SPAM-фильтра");
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", mailbox.getAccountId());

        List<UnixAccount> unixAccounts = (List<UnixAccount>) governorOfUnixAccount.buildAll(keyValue);
        if (unixAccounts.isEmpty()) {
            throw new ParameterValidationException("Не найдено UnixAccount для AccountID: " + mailbox.getAccountId());
        }

        Integer uid = unixAccounts.get(0).getUid();

        keyValue.put("resourceId", domainId);
        mailbox.setDomain(governorOfDomain.build(keyValue));

        try {
            mailbox.setPasswordHashByPlainPassword(plainPassword);
        } catch (UnsupportedEncodingException e) {
            throw new ParameterValidationException("Недопустимые символы в пароле");
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
            throw new ParameterValidationException("Внутренняя ошибка: не удалось сформировать mailSpool");
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
        mailbox.setComment(comment);

        return mailbox;
    }

    private Boolean hasAggregator(String domainId) {
        return repository.findByDomainIdAndIsAggregator(domainId, true) != null;
    }

    private String findMailStorageServer(String domainId) {
        List<Mailbox> mailboxes = repository.findByDomainId(domainId);

        try {
            return staffRcClient.getActiveMailboxServer().getId();
        } catch (FeignException e) {
            throw new ParameterValidationException("Внутренняя ошибка: не удалось найти подходящий сервер");
        }
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
    public void validate(Mailbox mailbox) throws ParameterValidationException {
        Set<ConstraintViolation<Mailbox>> constraintViolations = validator.validate(mailbox, MailboxChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("mailbox: " + mailbox + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(Mailbox mailbox) {
        Set<ConstraintViolation<Mailbox>> constraintViolations = validator.validate(mailbox, MailboxImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            logger.debug("[validateImported] mailbox: " + mailbox + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public Mailbox construct(Mailbox mailbox) throws ParameterValidationException {
        Domain domain;
        try {
            if (mailbox.getDomainId() != null) {
                domain = governorOfDomain.build(mailbox.getDomainId());
                mailbox.setDomain(domain);
            }
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }
        return mailbox;
    }

    private Mailbox constructForTe(Map<String, Domain> domainMap, Mailbox mailbox) {
        if (domainMap.containsKey(mailbox.getDomainId())) {
            mailbox.setDomain(domainMap.get(mailbox.getDomainId()));
        }
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

    public Collection<Mailbox> buildAllForTe(Map<String, String> keyValue) throws ResourceNotFoundException {
        List<Mailbox> mailboxes = new ArrayList<>();

        logger.info("[start] searchForDomains");

        DB db = mongoClient.getDB(springDataMongodbDatabase);

        Jongo jongo = new Jongo(db);
        MongoCollection domainsCollection = jongo.getCollection("domains");

        List<Domain> domains = new ArrayList<>();

        try (MongoCursor<Domain> domainsCursor = domainsCollection
                .find()
                .projection("{name: 1}")
                .as(Domain.class)
        ) {
            while (domainsCursor.hasNext()) {
                domains.add(domainsCursor.next());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.debug("[end] searchForDomains");

        Map<String, Domain> domainMap = domains.stream().collect(Collectors.toMap(Resource::getId, d -> d));

        if (keyValue.get("serverId") != null) {
            logger.debug("[start] searchForMailbox");

            MongoCollection mailboxesCollection = jongo.getCollection("mailboxes");

            try (MongoCursor<Mailbox> mailboxCursor = mailboxesCollection
                    .find("{serverId:#}", keyValue.get("serverId"))
                    .as(Mailbox.class)
            ) {
                while (mailboxCursor.hasNext()) {
                    mailboxes.add(constructForTe(domainMap, mailboxCursor.next()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            logger.debug("[end] searchForMailbox");
        }

        return mailboxes;
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
        String key = "mailboxUserData:" + mailbox.getFullNameInPunycode();
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
        redisRepository.delete(mailbox.getFullNameInPunycode());
        String key = "mailboxUserData:" + mailbox.getFullNameInPunycode();
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
        redisRepository.delete("*@" + IDN.toASCII((construct(mailbox)).getDomain().getName()));
    }

    public void updateQuota(String mailboxId, Long quotaSize) {
        Mailbox mailbox = build(mailboxId);

        // Сохраняем старые значения для определения необходимости отправки уведомлений
        long oldQuotaUsed = mailbox.getQuotaUsed();
        boolean oldWritable = mailbox.getWritable();

        //Устанавливаем новые квоту и writable после определения старых значений
        mailbox.setQuotaUsed(quotaSize);
        mailbox.setWritable(this.getNewWritable(mailbox));

        store(mailbox);
        syncWithRedis(mailbox);

        // Отправляем уведомление, если это необходимо

        notify(mailbox, oldWritable, oldQuotaUsed);
    }

    private boolean getNewWritable(Mailbox mailbox) {
        if (mailbox.getQuotaUsed() > mailbox.getQuota() && mailbox.getQuota() != 0) {
            return false;
        }

        UnixAccount unixAccount = unixAccountRepository.findFirstByAccountIdAndUid(mailbox.getAccountId(), mailbox.getUid());

        if (unixAccount == null  ) {
            throw new ResourceNotFoundException("UnixAccount с AccountId: " + mailbox.getAccountId() + " не найден");
        }

        return unixAccount.getWritable();
    }

    private void notify(Mailbox mailbox, boolean oldWritable, long oldQuotaUsed){
        if (!mailbox.getWritable() && oldWritable) {
            publisher.publishEvent(new MailboxQuotaFullEvent(mailbox));
        } else {

            int newQuotaUsedInPercent = ((Float) (((float) mailbox.getQuotaUsed()) * 100 / mailbox.getQuota())).intValue();
            int oldQuotaUsedInPercent = ((Float)(((float) oldQuotaUsed) * 100 / mailbox.getQuota())).intValue();

            if (newQuotaUsedInPercent >= warnPercent && oldQuotaUsedInPercent < warnPercent) {
                publisher.publishEvent(new MailboxQuotaWarnEvent(mailbox));
            }
        }
    }
}

package ru.majordomo.hms.rc.user.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

import com.mongodb.client.result.UpdateResult;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import feign.FeignException;
import org.springframework.util.Assert;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Storage;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.common.BuildResourceWithoutBuiltIn;
import ru.majordomo.hms.rc.user.common.ResourceAction;
import ru.majordomo.hms.rc.user.event.domain.DomainRedisSyncEvent;
import ru.majordomo.hms.rc.user.event.mailbox.MailboxRedisEvent;
import ru.majordomo.hms.rc.user.event.quota.MailboxQuotaFullEvent;
import ru.majordomo.hms.rc.user.event.quota.MailboxQuotaWarnEvent;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.repositories.*;
import ru.majordomo.hms.rc.user.repositoriesRedis.DkimRedisRepository;
import ru.majordomo.hms.rc.user.repositoriesRedis.MailboxRedisRepository;
import ru.majordomo.hms.rc.user.resources.*;
import ru.majordomo.hms.rc.user.resources.DTO.EntityIdOnly;
import ru.majordomo.hms.rc.user.resources.DTO.MailboxForRedis;
import ru.majordomo.hms.rc.user.resources.DTO.DkimRedis;
import ru.majordomo.hms.rc.user.resources.validation.group.MailboxChecks;
import ru.majordomo.hms.rc.user.resources.validation.group.MailboxImportChecks;

import static ru.majordomo.hms.rc.user.common.Constants.*;

@Service
public class GovernorOfMailbox extends LordOfResources<Mailbox> implements BuildResourceWithoutBuiltIn<Mailbox> {
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

    private String majordomoMailboxServerId;

    @Setter
    @Autowired
    private DKIMRepository dkimRepository;

    @Setter
    @Autowired
    private DkimRedisRepository dkimRedisRepository;

    @Setter
    @Autowired
    private MongoOperations mongoOperations;

    public GovernorOfMailbox(OperationOversightRepository<Mailbox> operationOversightRepository) {
        super(operationOversightRepository);
    }

    @Value("${resources.quotable.warnPercent.mailbox}")
    public void setWarnPercent(int warnPercent){
        this.warnPercent = warnPercent;
    }

    @Value("${resources.quotable.warnPercent.mailbox}")
    public void setMajordomoMailboxServerId(String majordomoMailboxServerId) {
        this.majordomoMailboxServerId = majordomoMailboxServerId;
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
    public OperationOversight<Mailbox> updateByOversight(ServiceMessage serviceMessage) throws ParameterValidationException, UnsupportedEncodingException {
        Mailbox mailbox = this.updateWrapper(serviceMessage);

        return sendToOversight(mailbox, ResourceAction.UPDATE);
    }

    /**
     * Синхронизация почтовых ящиков с Redis
     */
    public void syncAll() {
        try {
            Collection<EntityIdOnly> mailboxesIds = repository.findAllBy();
            for (EntityIdOnly mailboxIdOnly : mailboxesIds) {
                publisher.publishEvent(new MailboxRedisEvent(mailboxIdOnly));
            }
            Collection<EntityIdOnly> dkimIds = dkimRepository.findAllBy();
            for (EntityIdOnly dkimIdOnly : dkimIds) {
                publisher.publishEvent(new DomainRedisSyncEvent(dkimIdOnly));
            }
        } catch (Exception e) {
            log.error("We got an exception during synchronization all mailboxes and dkim with Redis", e);
        }
    }

    private Mailbox updateWrapper(ServiceMessage serviceMessage) throws UnsupportedEncodingException {
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
                    case IS_AGGREGATOR_KEY:
                        if (entry.getValue() != null) {
                            Boolean userValue = (Boolean) entry.getValue();
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
                        mailbox.setSwitchedOn((Boolean) entry.getValue());
                        break;
                    case "willBeDeletedAfter":
                        if (entry.getValue() == null) {
                            mailbox.setWillBeDeletedAfter(null);
                        } else {
                            mailbox.setWillBeDeletedAfter(LocalDateTime.parse((String) entry.getValue()));
                        }
                        break;
                    case "allowedIps":
                        mailbox.setAllowedIps(new HashSet<>(
                                cleaner.cleanListWithStrings((List<String>) entry.getValue()))
                        );
                        break;
                    default:
                        break;
                }
            }
        } catch (ClassCastException e) {
            throw new ParameterValidationException("Один из параметров указан неверно");
        }

        preValidate(mailbox);
        validate(mailbox);

        return mailbox;
    }

    @Override
    public void preDelete(String resourceId) {
        Mailbox mailbox = build(resourceId);
        dropFromRedis(mailbox);
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        if (!repository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Не найден почтовый ящик с ID: " + resourceId);
        }

        preDelete(resourceId);
        repository.deleteById(resourceId);
    }

    @Override
    public OperationOversight<Mailbox> dropByOversight(String resourceId) throws ResourceNotFoundException {
        Mailbox mailbox = build(resourceId);
        return sendToOversight(mailbox, ResourceAction.DELETE);
    }

    /**
     * Создание/Изменение ресурса и последущие удаление Oversight
     */
    @Override
    public Mailbox completeOversightAndStore(OperationOversight<Mailbox> ovs) {
        if (ovs.getReplace()) {
            removeOldResource(ovs.getResource());
        }
        Mailbox mailbox = ovs.getResource();
        store(mailbox);
        if (Boolean.TRUE.equals(mailbox.getIsAggregator())) {
            unmarkOtherAggregatorInMongo(mailbox);
        }
        construct(mailbox); //Добавляем транзиентный домен в мейлбокс, для синхронизации с редисом
        syncWithRedis(ovs.getResource());
        removeOversight(ovs);

        return mailbox;
    }

    @Override
    public Mailbox buildResourceFromServiceMessage(ServiceMessage serviceMessage)
            throws ClassCastException {
        Mailbox mailbox = new Mailbox();
        setResourceParams(mailbox, serviceMessage, cleaner);
        String plainPassword = null;
        String passwordHash = null;
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
        Set<String> allowedIps = new HashSet<>();
        @Nullable
        Boolean isAggregator = null;

        try {
            if (serviceMessage.getParam("domainId") == null) {
                throw new ParameterValidationException("Не указан domainId");
            }

            domainId = cleaner.cleanString((String) serviceMessage.getParam("domainId"));

            if (serviceMessage.getParam("password") != null) {
                plainPassword = (String) serviceMessage.getParam("password");
            }

            if (serviceMessage.getParam("passwordHash") != null) {
                passwordHash = (String) serviceMessage.getParam("passwordHash");
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

            if (serviceMessage.getParam("allowedIps") != null) {
                allowedIps = new HashSet<>(
                        cleaner.cleanListWithStrings(
                                (List<String>) serviceMessage.getParam("allowedIps")
                        )
                );
            }

            if (serviceMessage.getParam(IS_AGGREGATOR_KEY) != null) {
                isAggregator = (Boolean) serviceMessage.getParam(IS_AGGREGATOR_KEY);
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

        if (passwordHash != null) {
            mailbox.setPasswordHash(passwordHash);
        } else {
            if (plainPassword != null) {
                try {
                    Pattern p = Pattern.compile("^[a-zA-Z0-9 !\"#$%&\'\\\\()*+,\\-.\\/:;<=>?@\\[\\]^_`{|}~]{6,}$");
                    Matcher m = p.matcher(plainPassword);
                    if (!m.matches()) {
                        throw new ParameterValidationException("Недопустимые символы в пароле");
                    }
                } catch (Exception e) {
                    throw new ParameterValidationException("Недопустимые символы в пароле");
                }
            }

            try {
                mailbox.setPasswordHashByPlainPassword(plainPassword);
            } catch (UnsupportedEncodingException e) {
                throw new ParameterValidationException("Недопустимые символы в пароле");
            }
        }

        String serverId;

        if (governorOfDomain.build(domainId).getName() != null &&
                governorOfDomain.build(domainId).getName().equals(MAJORDOMO_SITE_NAME)) {
            serverId = findMjMailStorageServer();
        } else {
            serverId = findMailStorageServer();
        }

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
        mailbox.setAllowedIps(allowedIps);
        mailbox.setIsAggregator(isAggregator);

        return mailbox;
    }

    private String findMailStorageServer() {
        try {
            return staffRcClient.getActiveMailboxServer().getId();
        } catch (FeignException e) {
            throw new ParameterValidationException("Внутренняя ошибка: не удалось найти подходящий сервер");
        }
    }

    private String findMjMailStorageServer() {
        try {
            return staffRcClient.getActiveMjMailboxServer().getId();
        } catch (FeignException e) {
            throw new ParameterValidationException("Внутренняя ошибка: не удалось найти сервер внутренней почты");
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
            log.debug("mailbox: " + mailbox + " constraintViolations: " + constraintViolations.toString());
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    @Override
    public void validateImported(Mailbox mailbox) {
        Set<ConstraintViolation<Mailbox>> constraintViolations = validator.validate(mailbox, MailboxImportChecks.class);

        if (!constraintViolations.isEmpty()) {
            log.debug("[validateImported] mailbox: " + mailbox + " constraintViolations: " + constraintViolations.toString());
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

    @Override
    public Mailbox build(@Nonnull String resourceId, boolean withoutBuiltIn) throws ResourceNotFoundException {
        Mailbox mailbox = repository
                .findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Mailbox с ID:" + resourceId + " не найден"));

        return withoutBuiltIn ? mailbox : construct(mailbox);
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

        DB db = mongoClient.getDB(springDataMongodbDatabase);

        Jongo jongo = new Jongo(db);

        MongoCollection domainsCollection = jongo.getCollection("domains");

        log.info("[start] searchForDomains");

        Map<String, String> domainNames = new HashMap<>();


        try (MongoCursor<Domain> domainsCursor = domainsCollection
                .find()
                .projection("{name: 1}")
                .map(
                        result -> {
                            Domain domain = new Domain();
                            domain.setId(((ObjectId) result.get("_id")).toString());
                            domain.setName((String) result.get("name"));
                            return domain;
                        }
                )

        ) {
            while (domainsCursor.hasNext()) {
                Domain domain = domainsCursor.next();
                domainNames.put(domain.getId(), domain.getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info("[end] searchForDomains");

        if (keyValue.get("serverId") != null) {
            log.info("[start] searchForMailbox");

            MongoCollection mailboxesCollection = jongo.getCollection("mailboxes");

            try (MongoCursor<Mailbox> mailboxCursor = mailboxesCollection
                    .find("{serverId:#}", keyValue.get("serverId"))
                    .projection("{name: 1, uid: 1, mailSpool: 1, serverId: 1, domainId: 1}")
                    .map(
                            result -> {
                                Mailbox mailbox = new Mailbox();

                                if (result.get("_id") instanceof ObjectId) {
                                    mailbox.setId(((ObjectId) result.get("_id")).toString());
                                } else if (result.get("_id") instanceof String) {
                                    mailbox.setId((String) result.get("_id"));
                                }

                                mailbox.setName((String) result.get("name"));
                                mailbox.setUid((Integer) result.get("uid"));
                                mailbox.setMailSpool((String) result.get("mailSpool"));
                                mailbox.setServerId((String) result.get("serverId"));
                                mailbox.setDomainId((String) result.get("domainId"));

                                if (domainNames.get(mailbox.getDomainId()) != null) {
                                    Domain domain = new Domain();
                                    domain.setId(mailbox.getDomainId());
                                    domain.setName(domainNames.get(mailbox.getDomainId()));

                                    mailbox.setDomain(domain);
                                }

                                return mailbox;
                            }
                    )
            ) {
                while (mailboxCursor.hasNext()) {
                    mailboxes.add(mailboxCursor.next());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            log.info("[end] searchForMailbox");
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
        boolean writable = mailbox.getSwitchedOn() ? mailbox.getWritable() : false;
        Boolean mailFromAllowed = mailbox.getSwitchedOn() ? mailbox.getMailFromAllowed() : false;

        MailboxForRedis mailboxForRedis = new MailboxForRedis();
        String uidAsString = mailbox.getUid().toString();
        mailboxForRedis.setId(mailbox.getFullNameInPunycode());
        mailboxForRedis.setName(mailbox.getFullNameInPunycode());
        mailboxForRedis.setPasswordHash(mailbox.getPasswordHash());
        mailboxForRedis.setBlackList(String.join(":", mailbox.getBlackListInPunycode()));
        mailboxForRedis.setWhiteList(String.join(":", mailbox.getWhiteListInPunycode()));
        mailboxForRedis.setRedirectAddresses(String.join(":", mailbox.getRedirectAddressesInPunycode()));
        mailboxForRedis.setWritable(writable);
        mailboxForRedis.setMailFromAllowed(mailFromAllowed);
        mailboxForRedis.setAntiSpamEnabled(mailbox.getAntiSpamEnabled());
        mailboxForRedis.setSpamFilterAction(mailbox.getSpamFilterAction());
        mailboxForRedis.setSpamFilterMood(mailbox.getSpamFilterMood());
        mailboxForRedis.setServerName(serverName);
        mailboxForRedis.setStorageData(uidAsString + ":" + uidAsString + ":" + mailbox.getMailSpoolInPunycode());
        mailboxForRedis.setAllowedIps(String.join(":", mailbox.getAllowedIps()));

        return mailboxForRedis;
    }

    /**
     * Поместить в redis запись mailboxUserData:MAILBOX_FULL_NAME необходимую для работы IMAP и POP3-сервера dovecot
     * @param serverName адрес почтового сервера dovecot, например "pop1"
     */
    private void saveUserData(@Nonnull Mailbox mailbox, @Nonnull String serverName) {
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
            log.error("Mailbox userData не записана в Redis!");
        }
        String key = "mailboxUserData:" + mailbox.getFullNameInPunycode();
        redisTemplate.boundValueOps(key).set(data);
    }

    /**
     * @param mailbox
     * @throws IllegalArgumentException
     * @throws FeignException
     * @throws ResourceNotFoundException
     */
    @Override
    public void syncWithRedis(@Nonnull Mailbox mailbox) throws IllegalArgumentException, FeignException, ResourceNotFoundException {
        Server server = staffRcClient.getServerById(mailbox.getServerId());
        Assert.notNull(server, "Failed to get the mail server during sync mailbox with redis");
        String serverName = server.getName();

        updateAggregatorInRedis(mailbox, serverName);
        removeOrphanAggregatorInRedis(mailbox.getDomainId(), mailbox.getDomain() == null ? null : mailbox.getDomain().getName());

        redisRepository.save(convertMailboxToMailboxForRedis(mailbox, serverName));
        saveUserData(mailbox, serverName);
    }

    private void dropFromRedis(Mailbox mailbox) {
        Assert.notNull(mailbox.getDomain(), "Embedded domain object expected");
        String mailboxRedisId = MailboxForRedis.getRedisId(mailbox.getName(), mailbox.getDomain().getName());
        redisRepository.deleteById(mailboxRedisId);
        if (redisRepository.isAggregator(mailbox.getName(), mailbox.getDomain().getName())) {
            String aggregatorRedisId = MailboxForRedis.getAggregatorRedisId(mailbox.getDomain().getName());
            redisRepository.deleteById(aggregatorRedisId);
        }
        String key = "mailboxUserData:" + mailbox.getFullNameInPunycode();
        redisTemplate.delete(key);
    }

    /**
     * Удалить *@domain_name если если в mongo нет агрегатора
     * @param domainId
     * @param domainNameUnicode
     * @throws ResourceNotFoundException
     */
    private void removeOrphanAggregatorInRedis(@Nonnull String domainId, @Nullable String domainNameUnicode) throws ResourceNotFoundException {
        if (repository.existsByDomainIdAndIsAggregator(domainId, true)) {
            return;
        }
        if (domainNameUnicode == null) {
            domainNameUnicode = governorOfDomain.build(
                    domainId,
                    true
            ).getName();
        }
        String redisId = MailboxForRedis.getAggregatorRedisId(domainNameUnicode);
        redisRepository.deleteById(redisId);
    }

    public long unmarkOtherAggregatorInMongo(@Nonnull Mailbox unmarkExceptItMailbox) {
        ObjectId id = new ObjectId(unmarkExceptItMailbox.getId());
        Criteria criteria = Criteria.where("_id").ne(id).and(IS_AGGREGATOR_KEY).is(true)
                .and(DOMAIN_ID_KEY).is(unmarkExceptItMailbox.getDomainId());
        Query query = Query.query(criteria);
        Update update = Update.update(IS_AGGREGATOR_KEY, false);
        UpdateResult updateResult = mongoOperations.updateMulti(query, update, Mailbox.class);
        return updateResult.getModifiedCount();
    }

    /**
     * обновить запись агрегатора *@domain_name или удалить если ящик был агрегатором, но перестал
     * @param mailbox
     * @param serverName mailbox.serverId rc-staff.Server.name
     * @return true если что-то менялось в redis
     */
    public boolean updateAggregatorInRedis(@Nonnull Mailbox mailbox, @Nonnull String serverName) throws ResourceNotFoundException {
        Domain domain = mailbox.getDomain() != null ? mailbox.getDomain() : governorOfDomain.build(
                mailbox.getDomainId(),
                true
        );
        if (!Boolean.TRUE.equals(mailbox.getIsAggregator())) {
            if (Boolean.FALSE.equals(mailbox.getIsAggregator()) && redisRepository.isAggregator(mailbox.getName(), domain.getName())) {
                redisRepository.deleteById(MailboxForRedis.getAggregatorRedisId(domain.getName()));
                return true;
            } else {
                return false;
            }
        }

        Boolean writable = mailbox.getWritable();
        MailboxForRedis aggregatorRedis = MailboxForRedis.createAggregator(mailbox.getName(), domain.getName(), serverName, writable);

        redisRepository.deleteById(MailboxForRedis.getAggregatorRedisId(domain.getName())); // возможно не стоит удалять старую
        redisRepository.save(aggregatorRedis);
        return true;
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

        UnixAccount unixAccount = unixAccountRepository.findFirstByAccountId(mailbox.getAccountId());

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

    public void processQuotaReport(ServiceMessage serviceMessage) {
        String fullName = null, domainName = null, mailboxName = null, host = null;
        Long quotaUsed = null;

        if (serviceMessage.getParam("mailbox") != null) {
            fullName = (String) serviceMessage.getParam("mailbox");
        }

        if (serviceMessage.getParam("host") != null) {
            host = (String) serviceMessage.getParam("host");
        }

        if (serviceMessage.getParam("quotaUsed") != null) {
            Object quotaUsedFromMessage = serviceMessage.getParam("quotaUsed");
            if (quotaUsedFromMessage instanceof Long) {
                quotaUsed = (Long) serviceMessage.getParam("quotaUsed");
            } else if (quotaUsedFromMessage instanceof Integer) {
                quotaUsed = ((Integer) serviceMessage.getParam("quotaUsed")).longValue();
            }
        }

        DB db = mongoClient.getDB(springDataMongodbDatabase);

        Jongo jongo = new Jongo(db);

        MongoCollection mailboxesCollection = jongo.getCollection("mailboxes");
        MongoCollection domainsCollection = jongo.getCollection("domains");

        String[] splitFullName = fullName != null ? fullName.split("@", 2) : new String[0];

        if (splitFullName.length == 2) {
            mailboxName = splitFullName[0];
            domainName = splitFullName[1];
        }

        if (host != null && mailboxName != null && domainName != null && quotaUsed != null) {
            List<Server> servers = staffRcClient.getCachedServersOnlyIdAndNameByName(host);
            if (!servers.isEmpty()) {
                Domain currentDomain = domainsCollection
                        .findOne("{name: #}", java.net.IDN.toUnicode(domainName))
                        .projection("{name: 1}")
                        .map(
                                result -> {
                                    Domain domain = new Domain();
                                    domain.setId(((ObjectId) result.get("_id")).toString());
                                    domain.setName((String) result.get("name"));
                                    return domain;
                                }
                        );

                if (currentDomain != null) {
                    Mailbox currentMailbox = repository.findByNameAndDomainId(mailboxName, currentDomain.getId());

                    if(currentMailbox != null && currentMailbox.getServerId().equals(servers.get(0).getId()) && !currentMailbox.getQuotaUsed().equals(quotaUsed)) {
                        log.info("mailboxes quotaReport for host '" + host + "' and mailbox '" + fullName + "' found changed quotaUsed. old: " + currentMailbox.getQuotaUsed() + " new: " + quotaUsed);
                        currentMailbox.setDomain(currentDomain);

                        // Сохраняем старые значения для определения необходимости отправки уведомлений
                        long oldQuotaUsed = currentMailbox.getQuotaUsed();
                        boolean oldWritable = currentMailbox.getWritable();

                        //Устанавливаем новые квоту и writable после определения старых значений
                        currentMailbox.setQuotaUsed(quotaUsed);
                        currentMailbox.setWritable(this.getNewWritable(currentMailbox));

                        Object objectId = currentMailbox.getId();

                        try {
                            objectId = new ObjectId(currentMailbox.getId());
                        } catch (Exception ignored) {}

                        WriteResult writeResult = mailboxesCollection
                                .update("{_id: #}", objectId)
                                .with("{$set: {quotaUsed: #, writable: #}}", quotaUsed, currentMailbox.getWritable());

                        if (oldWritable != currentMailbox.getWritable()) {
                            syncWithRedis(currentMailbox);
                        }

                        // Отправляем уведомление, если это необходимо
                        notify(currentMailbox, oldWritable, oldQuotaUsed);
                    }
                }
            }
        }
    }

    /**
     * обновление и отключение dkim в redis
     * @param dkim null или dkim.isSwitchedOn отключить
     * @param domainNameUnicode можно в unicode
     */
    public void saveOnlyDkim(@Nullable DKIM dkim, @Nonnull String domainNameUnicode) {
        String privateKey = dkim == null ? null : dkim.getPrivateKey();
        if (dkim != null && dkim.isSwitchedOn()) {
            if (privateKey == null) {
                DKIM dkimPrivate = dkimRepository.findPrivateKeyOnly(dkim.getId());
                if (dkimPrivate == null || dkimPrivate.getPrivateKey() == null) {
                    log.error("cannot find dkim private key when attempt save to radis");
                    return;
                }
                privateKey = dkimPrivate.getPrivateKey();
            }

            DkimRedis dkimRedis = new DkimRedis();
            dkimRedis.setDkimSelector(dkim.getSelector());
            dkimRedis.setDkimKey(privateKey);
            dkimRedis.setId(DkimRedis.getRedisId(domainNameUnicode));
            dkimRedisRepository.save(dkimRedis);
        } else {
            dkimRedisRepository.deleteById(DkimRedis.getRedisId(domainNameUnicode));
        }
        
        log.debug("saveOnlyDkim switchedOn: {} for domain: {} with private key: {}", dkim == null ? null : dkim.isSwitchedOn(), domainNameUnicode, privateKey);
    }

}

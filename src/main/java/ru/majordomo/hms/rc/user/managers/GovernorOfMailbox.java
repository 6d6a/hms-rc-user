package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

import ru.majordomo.hms.rc.staff.resources.Server;
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
    public Resource update(ServiceMessage serviceMessage) throws ParameterValidateException {
        return null;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        Mailbox mailbox = new Mailbox();
        LordOfResources.setResourceParams(mailbox, serviceMessage, cleaner);

        String domainId = cleaner.cleanString((String) serviceMessage.getParam("domainId"));
        List<String> blackList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("blackList"));
        List<String> whilteList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("whiteList"));
        Long quota = (Long) serviceMessage.getParam("quota");
        Long quotaUsed = (Long) serviceMessage.getParam("quotaUsed");
        Boolean writable = (Boolean) serviceMessage.getParam("writable");
        String serverId = cleaner.cleanString((String)serviceMessage.getParam("serverId"));
        if (serverId == null) {
            serverId = staffRcClient.getActiveMailboxServers().get(0).getId();
        }
        Boolean antiSpamEnabled = (Boolean) serviceMessage.getParam("antiSpamEnabled");

        mailbox.setDomain((Domain) governorOfDomain.build(domainId));
        mailbox.setBlackList(blackList);
        mailbox.setWhiteList(whilteList);
        mailbox.setQuota(quota);
        mailbox.setQuotaUsed(quotaUsed);
        mailbox.setWritable(writable);
        mailbox.setServerId(serverId);
        mailbox.setAntiSpamEnabled(antiSpamEnabled);

        return mailbox;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        Mailbox mailbox = (Mailbox) resource;
        if (mailbox.getName() == null) {
            throw new ParameterValidateException("Имя ящика не может быть пустым");
        }

//        if (mailbox.getSize() > mailbox.getQuota()) {
//            throw new ParameterValidateException("Размер ящика не может быть больше квоты");
//        }

        if (mailbox.getDomain() == null) {
            throw new ParameterValidateException("Для ящика должен быть указан домен");
        }
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        Mailbox mailbox = repository.findOne(resourceId);
        if (mailbox == null) {
            throw new ResourceNotFoundException("Mailbox с ID:" + mailbox.getId() + " не найден");
        }

        Domain mailboxDomain = (Domain) governorOfDomain.build(mailbox.getDomainId());
        mailbox.setDomain(mailboxDomain);

        return mailbox;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        Collection<Mailbox> mailboxes = repository.findAll();
        for (Mailbox mailbox: mailboxes) {
            Domain domain = (Domain) governorOfDomain.build(mailbox.getDomainId());
            mailbox.setDomain(domain);
        }
        return mailboxes;
    }

    @Override
    public void store(Resource resource) {
        Mailbox mailbox = (Mailbox) resource;
        repository.save(mailbox);
    }

}

package ru.majordomo.hms.rc.user.managers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Service
public class GovernorOfUnixAccount extends LordOfResources {

    private final int MIN_UID = 2000;
    private final int MAX_UID = 40000000;

    private UnixAccountRepository repository;
    private Cleaner cleaner;
    private StaffResourceControllerClient staffRcClient;

    @Autowired
    public void setRepository(UnixAccountRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Autowired
    public void setStaffRcClient(StaffResourceControllerClient staffRcClient) {
        this.staffRcClient = staffRcClient;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        UnixAccount unixAccount;

        try {

            unixAccount = (UnixAccount) buildResourceFromServiceMessage(serviceMessage);
            validate(unixAccount);
            store(unixAccount);

        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return unixAccount;
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
        UnixAccount unixAccount = new UnixAccount();
        LordOfResources.setResourceParams(unixAccount, serviceMessage, cleaner);

        Integer uid = (Integer) serviceMessage.getParam("uid");
        if (uid == null) {
            uid = getFreeUid();
        }

        if (unixAccount.getName() == null) {
            unixAccount.setName("u" + uid);
        }

        String homeDir = cleaner.cleanString((String) serviceMessage.getParam("homeDir"));
        if (homeDir == null) {
            homeDir = "/home/" + unixAccount.getName();
        }

        String serverId = cleaner.cleanString((String) serviceMessage.getParam("serverId"));
        if (serverId == null) {
            serverId = getActiveHostingServerId();
        }

        List<CronTask> cronTasks = (List<CronTask>) serviceMessage.getParam("cronTasks");

        unixAccount.setUid(uid);
        unixAccount.setHomeDir(homeDir);
        unixAccount.setServerId(serverId);
        unixAccount.setCrontab(cronTasks);

        return unixAccount;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {
        UnixAccount unixAccount = (UnixAccount) resource;
        if (unixAccount.getName() == null) {
            throw new ParameterValidateException("Имя unixAccount'а не может быть пустым");
        }
        if (unixAccount.getUid() == null) {
            throw new ParameterValidateException("UID unixAccount'а не может быть пустым");
        }

        if (unixAccount.getHomeDir() == null) {
            throw new ParameterValidateException("homedir не может быть пустым");
        }

        if (unixAccount.getHomeDir().equals("/home") ||
                unixAccount.getHomeDir().equals("/home/") ||
                unixAccount.getHomeDir().equals("/")) {
            throw new ParameterValidateException("homedir не может быть /home или /");
        }
    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        UnixAccount unixAccount = repository.findOne(resourceId);
        return unixAccount;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        return repository.findAll();
    }

    @Override
    public void store(Resource resource) {
        UnixAccount unixAccount = (UnixAccount) resource;
        repository.save(unixAccount);
    }

    private String getActiveHostingServerId() {
        return staffRcClient.getActiveHostingServers().getId();
    }

    /*
        Функция для получения свободного UID. Алгоритмы получения (в порядке использования):
        1) получаем наибольший UID и добавляем к нему 1;
        2) получаем наименьший UID и вычитаем из него 1;
        3) получаем отсортированный список UID'ов пачками по 20 и сравниваем их разницу.
        Алгоритм 3 на примере:
        - список UID'ов 2000,2002,2005,2006,2007;
        - берем первую пару, т.е. UID'ы 2000 и 2002;
        - вычитаем из наибольшего наименьшее, т.е. 2002-2000;
        - результат операции вычитания 2, он не равен 1, значит в последовательности найдена бреш;
        - получаем число, которое пропущено, для этого берем минимальное и прибавляем к нему 1;
        - 2000+1=2001 - свободный UID.
        Если ни один из алгоритмов не сработал, скорее всего свободные UID'ы закончились, выбрасываем
        IllegalStateException.
     */
    private Integer getFreeUid() {
        UnixAccount unixAccount;

        // алгоритм 1
        unixAccount = repository.findFirstByOrderByUidDesc();
        Integer freeUid = unixAccount.getUid() + 1;

        // алгоритм 2
        if (!isUidValid(freeUid)) {
            unixAccount = repository.findFirstByOrderByUidAsc();
            freeUid = unixAccount.getUid() - 1;
        }

        // алгоритм 3
        if (!isUidValid(freeUid)) {
            Page<UnixAccount> page = repository.findAllByOrderByUidAsc(new PageRequest(0, 20));
            pageLoop:
            do {
                List<UnixAccount> unixAccountList = page.getContent();
                for (int i = 0; i < (unixAccountList.size() - 1); i++) {
                    Integer curUnixAccountUid = unixAccountList.get(i).getUid();
                    Integer nextUnixAccountUid = unixAccountList.get((i + 1)).getUid();
                    if ((nextUnixAccountUid - curUnixAccountUid) != 1 &&
                            isUidValid(curUnixAccountUid + 1)) {
                        freeUid = curUnixAccountUid + 1;
                        break pageLoop;
                    }
                }
                if (page.hasNext()) {
                    page = repository.findAllByOrderByUidAsc(page.nextPageable());
                }
            } while (page.hasNext());
        }

        if (!isUidValid(freeUid)) {
            throw new IllegalStateException("MIN_UID: " + MIN_UID + "\nMAX_UID: " + MAX_UID + "\nfreeUID: " + freeUid + " некорректен\nНе удалось найти корректный и свободный UID.");
        }

        return freeUid;
    }

    private Boolean isUidValid(Integer uid) {
        return (uid <= MAX_UID && uid >= MIN_UID);
    }

}

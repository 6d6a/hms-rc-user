package ru.majordomo.hms.rc.user.managers;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.rc.user.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@Service
public class GovernorOfUnixAccount extends LordOfResources {
    @Autowired
    UnixAccountRepository unixAccountRepository;
    @Value("${resources.unixAccount.maxUid}")
    Integer maxUid;
    @Value("${resources.unixAccount.minUid}")
    Integer minUid;
    @Value("${resources.unixAccount.namePattern}")
    String namePattern;
    @Value("${resources.unixAccount.homeDirPattern}")
    String homeDirPattern;

    @Override
    public Resource createResource(ServiceMessage serviceMessage) throws ParameterValidateException {
        Integer uid = getFreeUid();
        String name = namePattern + uid;
        String homeDir = homeDirPattern + "/" + name;
        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setUid(uid);
        unixAccount.setName(name);
        unixAccount.setHomeDir(homeDir);
        unixAccount.setHostingServer(getFreeHostingServer());
        return unixAccount;
    }

    public ObjectId getFreeHostingServer() {

        return new ObjectId();
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
    public Integer getFreeUid() {
        UnixAccount unixAccount = new UnixAccount();
        // алгоритм 1
        unixAccount = unixAccountRepository.findFirstByOrderByUidDesc();
        Integer freeUid = unixAccount.getUid() + 1;

        // алгоритм 2
        if (!isUidValid(freeUid)) {
            unixAccount = unixAccountRepository.findFirstByOrderByUidAsc();
            freeUid = unixAccount.getUid() - 1;
        }

        // алгоритм 3
        if (!isUidValid(freeUid)) {
            Page<UnixAccount> page = unixAccountRepository.findAllByOrderByUidAsc(new PageRequest(0, 20));
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
                    page = unixAccountRepository.findAllByOrderByUidAsc(page.nextPageable());
                }
            } while (page.hasNext());
        }

        if (!isUidValid(freeUid)) {
            throw new IllegalStateException("minUid: " + minUid + "\nmaxUid: " + maxUid + "\nfreeUID: " + freeUid + " некорректен\nНе удалось найти корректный и свободный UID.");
        }

        return freeUid;
    }

    public Boolean isUidValid(Integer uid) {

        return (uid <= maxUid && uid >= minUid);
    }

}

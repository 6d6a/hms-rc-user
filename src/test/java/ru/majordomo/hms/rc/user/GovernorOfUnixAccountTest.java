package ru.majordomo.hms.rc.user;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = UsersResourceControllerApplication.class)
public class GovernorOfUnixAccountTest {

    @Autowired
    UnixAccountRepository repository;
    @Autowired
    GovernorOfUnixAccount governorOfUnixAccount;
    @Value("${resources.unixAccount.minUid}")
    Integer minUid;
    @Value("${resources.unixAccount.maxUid}")
    Integer maxUid;


    @Test
    public void createTest() {
        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setUid(12345);
        unixAccount.setName("u134035");
        unixAccount.setHomeDir("/home/u134035");
        repository.save(unixAccount);
        System.out.println(unixAccount.toString());
    }
    /*
        Самая простая ситуация, когда для получения UID'а нужно взять следующий по порядку
        за максимальным.
     */
    @Test
    public void getFreeUidLowerBoundTest() {
        try {
            repository.deleteAll();
            generateUnixAccountDataBagByUidRange(minUid, minUid + 10);
            Assert.assertEquals((Integer) (minUid + 11), governorOfUnixAccount.getFreeUid());
        } finally {
            repository.deleteAll();
        }
    }

    /*
        Другая простая ситуация, когда для получения UID'а нужно взять предыдущий за наименьшим
     */
    @Test
    public void getFreeUidUpperBoundTest() {
        try {
            repository.deleteAll();
            generateUnixAccountDataBagByUidRange(maxUid - 10, maxUid);
            Assert.assertEquals((Integer) (maxUid - 11), governorOfUnixAccount.getFreeUid());
        } finally {
            repository.deleteAll();
        }
    }

    /*
        Верхний и нижний UID'ы заняты, поэтому придется получать UID из середины
     */
    @Test
    public void getFreeUidMiddleTest() {
        try {
            repository.deleteAll();
            generateUnixAccountDataBagByUidRange(minUid, minUid + 10);
            generateUnixAccountDataBagByUidRange(maxUid - 10, maxUid);
            generateUnixAccountDataBagByUidRange(minUid + 12, minUid + 20);
            Assert.assertEquals((Integer) (minUid + 11), governorOfUnixAccount.getFreeUid());
        } finally {
            repository.deleteAll();
        }
    }

    /*
        Проверяем корректность проверки UID'а.
        Для этого пробуем валидировать типичные ситуации:
     */
    @Test
    public void isUidCorrectTest() {
        Assert.assertFalse(governorOfUnixAccount.isUidValid(minUid-1)); //UID меньше resources.unixAccount.minUid;
        Assert.assertTrue(governorOfUnixAccount.isUidValid(minUid)); //UID равен resources.unixAccount.minUid;
        Assert.assertTrue(governorOfUnixAccount.isUidValid(minUid+1)); //UID больше resources.unixAccount.minUid и меньше resources.unixAccount.maxUid;
        Assert.assertTrue(governorOfUnixAccount.isUidValid(maxUid)); //UID равен resources.unixAccount.maxUid;
        Assert.assertFalse(governorOfUnixAccount.isUidValid(maxUid+1)); //UID больше resources.unixAccount.maxUid;
        Assert.assertTrue(maxUid > minUid); //maxUid больше, чем minUid

    }

    private void generateUnixAccountDataBagByUidRange(Integer min, Integer max) {
        List<UnixAccount> unixAccountList = new ArrayList<>();
        for (int i = min; i <= max && i > 0; i++) {
            UnixAccount unixAccount = new UnixAccount();
            unixAccount.setUid(i);
            unixAccountList.add(unixAccount);
        }
        repository.save(unixAccountList);
    }
}

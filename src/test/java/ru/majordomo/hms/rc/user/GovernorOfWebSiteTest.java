package ru.majordomo.hms.rc.user;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.CharSet;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = UsersResourceControllerApplication.class)
public class GovernorOfWebSiteTest {

    private static final Logger logger = LoggerFactory.getLogger(GovernorOfWebSiteTest.class);
    @Autowired
    DomainRepository domainRepository;
    @Autowired
    UnixAccountRepository unixAccountRepository;
    @Autowired
    WebSiteRepository webSiteRepository;
    @Autowired
    GovernorOfWebSite governorOfWebSite;

    @Test
    public void createTest() {

        try {
            unixAccountRepository.deleteAll();
            domainRepository.deleteAll();
            webSiteRepository.deleteAll();

            // создаем домен
            Domain domain = new Domain();
            domain.setName("majordomo.ru");
            domainRepository.save(domain);

            // создаем UNIX account
            UnixAccount unixAccount = new UnixAccount();
            unixAccount.setName("u134035");
            unixAccount.setHomeDir("/home/u134035");
            unixAccount.setUid(34005);
            unixAccountRepository.save(unixAccount);

            ServiceMessage serviceMessage = new ServiceMessage();
            serviceMessage.setActionIdentity(ObjectId.get().toString());
            serviceMessage.setOperationIdentity(ObjectId.get().toString());
            serviceMessage.setObjRef(null);

            List<String> domainIds = new ArrayList<>();
            domainIds.add(domain.getId());
            serviceMessage.addParam("domainIds",domainIds);

            serviceMessage.addParam("name","домен для Majordomo");

            serviceMessage.addParam("applicationServerId",ObjectId.get().toString());

            serviceMessage.addParam("documentRoot","/majordomo.ru/www");

            serviceMessage.addParam("unixAccountId",unixAccount.getId());

            serviceMessage.addParam("charSet", CharSet.UTF8.toString());

            serviceMessage.addParam("ssiEnabled", Boolean.FALSE);

            List<String> ssiFileExtensions = new ArrayList<>();
            serviceMessage.addParam("ssiFileExtensions",ssiFileExtensions);

            serviceMessage.addParam("cgiEnabled",Boolean.TRUE);

            List<String> cgiFileExtensions = new ArrayList<>();
            cgiFileExtensions.add("cgi");
            cgiFileExtensions.add("perl");
            serviceMessage.addParam("cgiFileExtensions",cgiFileExtensions);

            serviceMessage.addParam("scriptAlias","/cgi-bin");

            serviceMessage.addParam("autoSubDomain",Boolean.TRUE);

            serviceMessage.addParam("accessByOldHttpVersion",Boolean.FALSE);

            List<String> staticFileExtensions = new ArrayList<>();
            staticFileExtensions.add("png");
            staticFileExtensions.add("jpg");
            staticFileExtensions.add("htm");
            serviceMessage.addParam("staticFileExtensions",staticFileExtensions);

            List<String> indexFileList = new ArrayList<>();
            indexFileList.add("index.html");
            indexFileList.add("index.php");
            serviceMessage.addParam("indexFileList",indexFileList);

            serviceMessage.addParam("accessLogEnabled",Boolean.TRUE);
            serviceMessage.addParam("errorLogEnabled",Boolean.TRUE);
            logger.warn(serviceMessage.toString());
            try {
                WebSite webSite = (WebSite)governorOfWebSite.createResource(serviceMessage);
                logger.warn(webSite.toString());
                logger.warn(unixAccount.toString());
                logger.warn(domain.toString());
            } catch (ParameterValidateException e) {
                e.printStackTrace();
            }

        } finally {
//            unixAccountRepository.deleteAll();
//            domainRepository.deleteAll();
//            webSiteRepository.deleteAll();
        }
    }


}

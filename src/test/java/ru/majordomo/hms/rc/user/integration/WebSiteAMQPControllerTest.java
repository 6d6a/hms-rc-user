package ru.majordomo.hms.rc.user.integration;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.UsersResourceControllerApplication;
import ru.majordomo.hms.rc.user.api.amqp.Sender;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.CharSet;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = UsersResourceControllerApplication.class)
public class WebSiteAMQPControllerTest {
    @Autowired
    UnixAccountRepository unixAccountRepository;

    @Autowired
    DomainRepository domainRepository;

    @Autowired
    WebSiteRepository webSiteRepository;

    @Autowired
    Sender sender;

    @Test
    public void kickExecutor() {
        ServiceMessage serviceMessage = new ServiceMessage();
        serviceMessage.setObjRef("http://localhost:8000/rc/website/57d02e1932eab4c14d4fde28");
        serviceMessage.setActionIdentity(ObjectId.get().toString());
        serviceMessage.setOperationIdentity(ObjectId.get().toString());
        sender.send("website.create","te.web21",serviceMessage);
    }

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
            serviceMessage.addParam("domainIds", domainIds);

            serviceMessage.addParam("name", "домен для Majordomo");

            serviceMessage.addParam("applicationServerId", ObjectId.get().toString());

            serviceMessage.addParam("documentRoot", "/majordomo.ru/www");

            serviceMessage.addParam("unixAccountId", unixAccount.getId());

            serviceMessage.addParam("charSet", CharSet.UTF8.toString());

            serviceMessage.addParam("ssiEnabled", Boolean.FALSE);

            List<String> ssiFileExtensions = new ArrayList<>();
            serviceMessage.addParam("ssiFileExtensions", ssiFileExtensions);

            serviceMessage.addParam("cgiEnabled", Boolean.FALSE);

            List<String> cgiFileExtensions = new ArrayList<>();
            serviceMessage.addParam("cgiFileExtensions", cgiFileExtensions);

            serviceMessage.addParam("scriptAlias", "/cgi-bin");

            serviceMessage.addParam("autoSubDomain", Boolean.TRUE);

            serviceMessage.addParam("accessByOldHttpVersion", Boolean.FALSE);

            List<String> staticFileExtensions = new ArrayList<>();
            staticFileExtensions.add("png");
            staticFileExtensions.add("jpg");
            staticFileExtensions.add("htm");
            serviceMessage.addParam("staticFileExtensions", staticFileExtensions);

            List<String> indexFileList = new ArrayList<>();
            indexFileList.add("index.html");
            indexFileList.add("index.php");
            serviceMessage.addParam("indexFileList", indexFileList);

            serviceMessage.addParam("accessLogEnabled", Boolean.TRUE);
            serviceMessage.addParam("errorLogEnabled", Boolean.TRUE);

            sender.send("website.create","rc-user",serviceMessage);
        } finally {
            unixAccountRepository.deleteAll();
            domainRepository.deleteAll();
            webSiteRepository.deleteAll();
        }

    }
}


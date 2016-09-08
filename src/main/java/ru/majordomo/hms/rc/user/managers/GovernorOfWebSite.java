package ru.majordomo.hms.rc.user.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.Resource;
import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.cleaner.Cleaner;
import ru.majordomo.hms.rc.user.common.CharSet;
import ru.majordomo.hms.rc.user.exception.ParameterValidateException;
import ru.majordomo.hms.rc.user.repositories.DomainRepository;
import ru.majordomo.hms.rc.user.repositories.UnixAccountRepository;
import ru.majordomo.hms.rc.user.repositories.WebSiteRepository;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

@Component
public class GovernorOfWebSite extends LordOfResources {
    private Logger logger = LoggerFactory.getLogger(GovernorOfWebSite.class);

    @Autowired
    DomainRepository domainRepository;

    @Autowired
    WebSiteRepository webSiteRepository;

    @Autowired
    UnixAccountRepository unixAccountRepository;

    @Autowired
    Cleaner cleaner;

    @Override
    public Resource createResource(ServiceMessage serviceMessage) throws ParameterValidateException {
        String loggerPrefix = "OPERATION IDENTITY:" + serviceMessage.getOperationIdentity() + " ACTION IDENTITY:" + serviceMessage.getActionIdentity() + " ";
        List<Domain> domains = new ArrayList<>();

        WebSite webSite = new WebSite();

        String name = cleaner.cleanString((String)serviceMessage.getParam("name"));

        if (name != "") {
            webSite.setName(name);
        } else {
            logger.warn(loggerPrefix + "'name' parameter is null");
        }

        for (String domainId: (List<String>)serviceMessage.getParam("domainIds")) {
            Domain domain = domainRepository.findOne(domainId);
            if (domain == null) {
                throw new ParameterValidateException(loggerPrefix + "Domain with ID " + domainId + " not found");
            } else {
                logger.info(loggerPrefix + "Добавлен домен. Имя:" + domain.getName() + " Id:" + domain.getId() );
                webSite.addDomain(domain);
            }
        }

        String applicationServerId = cleaner.cleanString((String)serviceMessage.getParam("applicationServerId"));
        if (applicationServerId != "") {
            //TODO добавить проверку на наличие такого application server'а и проверку на тот факт, что это сервис закреплен за нужным сервером
            webSite.setApplicationServer(applicationServerId);
        } else {
            throw new ParameterValidateException(loggerPrefix + "applicationServerId не может быть пустым");
        }

        String documentRoot = cleaner.cleanString((String)serviceMessage.getParam("documentRoot"));
        if (documentRoot != "") {
            webSite.setDocumentRoot(documentRoot);
        } else {
            throw new ParameterValidateException(loggerPrefix + "documentRoot не может быть пустым");
        }

        String unixAccountId = cleaner.cleanString((String)serviceMessage.getParam("unixAccountId"));
        if (unixAccountId != "") {
            UnixAccount unixAccount = unixAccountRepository.findOne(unixAccountId);
            if (unixAccount == null) {
                throw new ParameterValidateException(loggerPrefix + "unixAccount с ID:" + unixAccountId + " не существует");
            } else {
                webSite.setUnixAccount(unixAccount);
            }
        }

        String charsetAsString = cleaner.cleanString((String)serviceMessage.getParam("charSet"));
        if (charsetAsString != "") {
            try {
                CharSet charSet = Enum.valueOf(CharSet.class, charsetAsString);
                webSite.setCharSet(charSet);
            } catch (IllegalArgumentException ex) {
                throw new ParameterValidateException(loggerPrefix + "Кодировка:" + charsetAsString + " недопустима");
            }
        } else {
            throw new ParameterValidateException(loggerPrefix + "charSet не может быть пустым");
        }

        try {
            Boolean ssiEnabled = (Boolean) serviceMessage.getParam("ssiEnabled");
            webSite.setSsiEnabled(ssiEnabled);
        } catch (ClassCastException ex) {
            throw new ParameterValidateException(loggerPrefix + "ssiEnabled должен иметь булево значение");
        }

        List<String> ssiFileExtensions = new ArrayList<>();
        ssiFileExtensions = cleaner.cleanListWithStrings((List<String>)serviceMessage.getParam("ssiFileExtensions"));
        webSite.setSsiFileExtensions(ssiFileExtensions);

        try {
            Boolean cgiEnabled = (Boolean) serviceMessage.getParam("cgiEnabled");
            webSite.setCgiEnabled(cgiEnabled);
        } catch (ClassCastException ex) {
            throw new ParameterValidateException(loggerPrefix + "cgiEnabled должен иметь булево значение");
        }

        List<String> cgiFileExtensions = new ArrayList<>();
        cgiFileExtensions = cleaner.cleanListWithStrings((List<String>)serviceMessage.getParam("cgiFileExtensions"));
        webSite.setCgiFileExtensions(cgiFileExtensions);

        String scriptAlias = cleaner.cleanString((String)serviceMessage.getParam("scriptAlias"));
        if (scriptAlias != "") {
            webSite.setScriptAlias(scriptAlias);
        }

        try {
            Boolean autoSubDomain = (Boolean)serviceMessage.getParam("autoSubDomain");
            webSite.setAutoSubDomain(autoSubDomain);
        } catch (ClassCastException ex) {
            throw new ParameterValidateException(loggerPrefix + "autoSubDomain должен иметь булево значение");
        }

        try {
            Boolean accessByOldHttpVersion = (Boolean) serviceMessage.getParam("accessByOldHttpVersion");
            webSite.setAccessByOldHttpVersion(accessByOldHttpVersion);
        } catch (ClassCastException ex) {
            throw new ParameterValidateException(loggerPrefix + "accessByOldHttpVersion должен иметь булево значение");
        }

        List<String> staticFileExtensions = new ArrayList<>();
        staticFileExtensions = cleaner.cleanListWithStrings((List<String>)serviceMessage.getParam("staticFileExtensions"));
        webSite.setStaticFileExtensions(staticFileExtensions);

        List<String> indexFileList = new ArrayList<>();
        indexFileList = cleaner.cleanListWithStrings((List<String>)serviceMessage.getParam("indexFileList"));
        webSite.setIndexFileList(indexFileList);

        try {
            Boolean accessLogEnabled = (Boolean) serviceMessage.getParam("accessLogEnabled");
            webSite.setAccessLogEnabled(accessLogEnabled);
        } catch (ClassCastException ex) {
            throw new ParameterValidateException(loggerPrefix + "accessLogEnabled должен иметь булево значение");
        }

        try {
            Boolean errorLogEnabled = (Boolean) serviceMessage.getParam("errorLogEnabled");
            webSite.setErrorLogEnabled(errorLogEnabled);
        } catch (ClassCastException ex) {
            throw new ParameterValidateException(loggerPrefix + "errorLogEnabled должен иметь булево значение");
        }

        webSiteRepository.save(webSite);
        return webSite;
    }
}

package ru.majordomo.hms.rc.user.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ru.majordomo.hms.rc.user.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.Resource;
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

    private WebSiteRepository repository;
    private GovernorOfDomain governorOfDomain;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private Cleaner cleaner;

    @Autowired
    public void setGovernorOfUnixAccount(GovernorOfUnixAccount governorOfUnixAccount) {
        this.governorOfUnixAccount = governorOfUnixAccount;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Autowired
    public void setRepository(WebSiteRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setCleaner(Cleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Override
    public Resource create(ServiceMessage serviceMessage) throws ParameterValidateException {
        WebSite webSite;
        try {
            webSite = (WebSite) buildResourceFromServiceMessage(serviceMessage);
            validate(webSite);
            store(webSite);
        } catch (ClassCastException e) {
            throw new ParameterValidateException("Один из параметров указан неверно:" + e.getMessage());
        }

        return webSite;
    }

    @Override
    public void drop(String resourceId) throws ResourceNotFoundException {
        repository.delete(resourceId);
    }

    @Override
    protected Resource buildResourceFromServiceMessage(ServiceMessage serviceMessage) throws ClassCastException {
        WebSite webSite = new WebSite();

        LordOfResources.setResourceParams(webSite, serviceMessage, cleaner);

        List<String> domainIds = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("domainIds"));
        for (String domainId : domainIds) {
            Domain domain = (Domain) governorOfDomain.build(domainId);
            webSite.addDomain(domain);
        }

        String applicationServerId = cleaner.cleanString((String) serviceMessage.getParam("applicationServerId"));
        String documentRoot = cleaner.cleanString((String) serviceMessage.getParam("documentRoot"));

        String unixAccountId = cleaner.cleanString((String) serviceMessage.getParam("unixAccountId"));
        UnixAccount unixAccount = (UnixAccount) governorOfUnixAccount.build(unixAccountId);

        String charsetAsString = cleaner.cleanString((String) serviceMessage.getParam("charSet"));
        CharSet charSet = Enum.valueOf(CharSet.class, charsetAsString);

        Boolean ssiEnabled = (Boolean) serviceMessage.getParam("ssiEnabled");
        List<String> ssiFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("ssiFileExtensions"));
        Boolean cgiEnabled = (Boolean) serviceMessage.getParam("cgiEnabled");
        List<String> cgiFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("cgiFileExtensions"));
        String scriptAlias = cleaner.cleanString((String) serviceMessage.getParam("scriptAlias"));
        Boolean autoSubDomain = (Boolean) serviceMessage.getParam("autoSubDomain");
        Boolean accessByOldHttpVersion = (Boolean) serviceMessage.getParam("accessByOldHttpVersion");
        List<String> staticFileExtensions = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("staticFileExtensions"));
        List<String> indexFileList = cleaner.cleanListWithStrings((List<String>) serviceMessage.getParam("indexFileList"));
        Boolean accessLogEnabled = (Boolean) serviceMessage.getParam("accessLogEnabled");
        Boolean errorLogEnabled = (Boolean) serviceMessage.getParam("errorLogEnabled");

        webSite.setServerId(applicationServerId);
        webSite.setDocumentRoot(documentRoot);
        webSite.setUnixAccount(unixAccount);
        webSite.setCharSet(charSet);
        webSite.setSsiEnabled(ssiEnabled);
        webSite.setSsiFileExtensions(ssiFileExtensions);
        webSite.setCgiEnabled(cgiEnabled);
        webSite.setCgiFileExtensions(cgiFileExtensions);
        webSite.setScriptAlias(scriptAlias);
        webSite.setAutoSubDomain(autoSubDomain);
        webSite.setAccessByOldHttpVersion(accessByOldHttpVersion);
        webSite.setStaticFileExtensions(staticFileExtensions);
        webSite.setIndexFileList(indexFileList);
        webSite.setAccessLogEnabled(accessLogEnabled);
        webSite.setErrorLogEnabled(errorLogEnabled);

        return webSite;
    }

    @Override
    public void validate(Resource resource) throws ParameterValidateException {

    }

    @Override
    public Resource build(String resourceId) throws ResourceNotFoundException {
        WebSite webSite = repository.findOne(resourceId);
        for (String domainId : webSite.getDomainIds()) {
            Domain domain = (Domain) governorOfDomain.build(domainId);
            webSite.addDomain(domain);
        }

        String unixAccountId = webSite.getUnixAccountId();
        UnixAccount unixAccount = (UnixAccount) governorOfUnixAccount.build(unixAccountId);
        webSite.setUnixAccount(unixAccount);

        return webSite;
    }

    @Override
    public Collection<? extends Resource> buildAll() {
        List<WebSite> webSites = new ArrayList<>();
        webSites = repository.findAll();
        for (WebSite webSite : webSites) {
            for (String domainId : webSite.getDomainIds()) {
                Domain domain = (Domain) governorOfDomain.build(domainId);
                webSite.addDomain(domain);
            }

            String unixAccountId = webSite.getUnixAccountId();
            UnixAccount unixAccount = (UnixAccount) governorOfUnixAccount.build(unixAccountId);
            webSite.setUnixAccount(unixAccount);
        }
        return webSites;
    }

    @Override
    public void store(Resource resource) {

    }

}

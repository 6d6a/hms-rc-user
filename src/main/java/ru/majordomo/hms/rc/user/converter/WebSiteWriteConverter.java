package ru.majordomo.hms.rc.user.converter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.sun.tools.javac.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.util.ArrayList;

import ru.majordomo.hms.rc.user.Resource;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.WebSite;

@WritingConverter
public class WebSiteWriteConverter implements Converter<WebSite, DBObject> {

    @Autowired
    MongoConverter mongoConverter;

    @Override
    public DBObject convert(WebSite webSite) {
        //WebSite.class.getFields();
        DBObject dbo = new BasicDBObject();
        dbo.put("_id", webSite.getId());
        dbo.put("name", webSite.getName());
        dbo.put("unixAccount", webSite.getUnixAccount().getId());
        dbo.put("applicationServer", webSite.getApplicationServer());
        dbo.put("documentRoot", webSite.getDocumentRoot());

        List<String> domainsIdList = new ArrayList<>();
        for (Resource domain: webSite.getDomains()) {
            domainsIdList.add(domain.getId());
        }
        dbo.put("domainList",domainsIdList);

        dbo.put("charSet", webSite.getCharSet());
        dbo.put("ssiEnabled", webSite.getSsiEnabled());
        dbo.put("ssiFileExtensions", webSite.getSsiFileExtensions());
        dbo.put("cgiEnabled", webSite.getCgiEnabled());
        dbo.put("cgiFileExtensions", webSite.getCgiFileExtensions());
        dbo.put("scriptAlias", webSite.getScriptAlias());
        dbo.put("ddosProtection", webSite.getDdosProtection());
        dbo.put("autoSubDomain", webSite.getAutoSubDomain());
        dbo.put("accessByOldHttpVersion",webSite.getAccessByOldHttpVersion());
        return dbo;
    }
}

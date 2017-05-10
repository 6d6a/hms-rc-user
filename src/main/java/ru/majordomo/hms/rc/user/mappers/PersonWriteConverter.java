package ru.majordomo.hms.rc.user.mappers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import ru.majordomo.hms.rc.user.resources.Person;

@WritingConverter
public class PersonWriteConverter implements Converter<Person, DBObject>{
    @Override
    public DBObject convert(Person source) {
        DBObject dbo = new BasicDBObject();
        dbo.put("_id", source.getId());
        dbo.put("name", source.getName());
        dbo.put("accountId", source.getAccountId());
        dbo.put("switchedOn", source.getSwitchedOn());
        dbo.put("emailAdresses", source.getEmailAddresses());
        dbo.put("phoneNumbers", source.getPhoneNumbers());
        dbo.put("passport", source.getPassport());
        dbo.put("legalEntity", source.getLegalEntity());
        dbo.put("country", source.getCountry());
        dbo.put("postalAddress", source.getPostalAddressAsString());
        dbo.put("nicHandle", source.getNicHandle());
        dbo.put("linkedAccountIds", source.getLinkedAccountIds());

        return dbo;
    }
}

package ru.majordomo.hms.rc.user.event.webSite.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.rc.user.resources.WebSite;

@Component
public class WebsiteMongoEventListener extends AbstractMongoEventListener<WebSite> {
    private StaffResourceControllerClient staffRcClient;
    private final MongoOperations mongoOperations;

    @Autowired
    public WebsiteMongoEventListener(
            MongoOperations mongoOperations,
            StaffResourceControllerClient staffRcClient
    ) {
        this.mongoOperations = mongoOperations;
        this.staffRcClient = staffRcClient;
    }

    @Override
    public void onAfterSave(AfterSaveEvent<WebSite> event) {
        super.onAfterSave(event);
        WebSite webSite = event.getSource();

        if (webSite.getServiceId() != null) {
            Service service = staffRcClient.getService(webSite.getServiceId());

            if (service != null && service.getTemplate().getResourceFilter() != null) {
                webSite.setResourceFilter(service.getTemplate().getResourceFilter());

                Query query = new Query(new Criteria("_id").is(webSite.getId()));
                Update update = new Update().set("resourceFilter", service.getTemplate().getResourceFilter());

                mongoOperations.updateFirst(query, update, WebSite.class);
            }
        }
    }


}

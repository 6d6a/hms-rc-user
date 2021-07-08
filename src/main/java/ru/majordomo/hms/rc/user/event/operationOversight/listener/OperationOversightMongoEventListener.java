package ru.majordomo.hms.rc.user.event.operationOversight.listener;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.model.OperationOversight;
import ru.majordomo.hms.rc.user.resources.Resource;

@Component
public class OperationOversightMongoEventListener<T extends Resource> extends AbstractMongoEventListener<OperationOversight<T>> {
    @Override
    public void onBeforeConvert(BeforeConvertEvent<OperationOversight<T>> event) {
        super.onBeforeConvert(event);
        OperationOversight operationOversight = event.getSource();
        operationOversight.generateHashes();
    }
}

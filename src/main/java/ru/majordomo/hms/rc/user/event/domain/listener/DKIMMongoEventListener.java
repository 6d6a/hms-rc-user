package ru.majordomo.hms.rc.user.event.domain.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.common.DKIMManager;
import ru.majordomo.hms.rc.user.resources.DKIM;

import javax.annotation.Nonnull;

@Component
@RequiredArgsConstructor
public class DKIMMongoEventListener extends AbstractMongoEventListener<DKIM> {
    @Value("${resources.dkim.contentPattern}")
    final private String dkimContentPattern;

    @Override
    public void onAfterConvert(@Nonnull AfterConvertEvent<DKIM> event) {
        super.onAfterConvert(event);
        DKIM dkim = event.getSource();
        if (dkim.getPublicKey() == null) return;
        dkim.setData(DKIMManager.makeContent(dkim.getPublicKey(), dkimContentPattern));
    }
}

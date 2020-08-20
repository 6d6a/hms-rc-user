package ru.majordomo.hms.rc.user.event.domain.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.rc.user.repositories.DKIMRepository;
import ru.majordomo.hms.rc.user.resources.DKIM;
import ru.majordomo.hms.rc.user.resources.Domain;

import javax.annotation.Nonnull;

@Component
@RequiredArgsConstructor
public class DomainMongoEventListener extends AbstractMongoEventListener<Domain> {
    private final DKIMRepository dkimRepository;

    @Override
    public void onAfterConvert(@Nonnull AfterConvertEvent<Domain> event) {
        super.onAfterConvert(event);
        Domain domain = event.getSource();
        DKIM dkim = dkimRepository.findWithoutPrivateKey(domain.getId());
        if (dkim != null) {
            domain.setDkim(dkim);
        }
    }
}

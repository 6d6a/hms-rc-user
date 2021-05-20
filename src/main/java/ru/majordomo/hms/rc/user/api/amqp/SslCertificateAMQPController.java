package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.common.Constants;
import ru.majordomo.hms.rc.user.common.ResourceActionContext;
import ru.majordomo.hms.rc.user.managers.GovernorOfDomain;
import ru.majordomo.hms.rc.user.managers.GovernorOfSSLCertificate;
import ru.majordomo.hms.rc.user.resourceProcessor.ResourceProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl.SSLCertificateCreateFromLetsEncryptProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl.SSLCertificateCreatePmProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl.SSLCertificateDeletePmProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl.SSLCertificateUpdateFromLetsEncrypt;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.ssl.SSLCertificateUpdateFromPm;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.te.TeCreateProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.te.TeDeleteProcessor;
import ru.majordomo.hms.rc.user.resourceProcessor.impl.te.TeUpdateProcessor;
import ru.majordomo.hms.rc.user.resources.SSLCertificate;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_CREATE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_DELETE;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.SSL_CERTIFICATE_UPDATE;
import static ru.majordomo.hms.rc.user.common.Constants.LETSENCRYPT;
import static ru.majordomo.hms.rc.user.common.Constants.PM;
import static ru.majordomo.hms.rc.user.common.Constants.TE;

@Service
public class SslCertificateAMQPController extends BaseAMQPController<SSLCertificate> {

    private GovernorOfDomain governorOfDomain;

    @Autowired
    public void setGovernor(GovernorOfSSLCertificate governor) {
        this.governor = governor;
    }

    @Autowired
    public void setGovernorOfDomain(GovernorOfDomain governorOfDomain) {
        this.governorOfDomain = governorOfDomain;
    }

    @Override
    protected ResourceProcessor<SSLCertificate> getEventProcessor(ResourceActionContext<SSLCertificate> context) {
        switch (context.getAction()) {
            case CREATE:
                switch (context.getEventProvider()) {
                    case PM:
                        return new SSLCertificateCreatePmProcessor(this, governorOfDomain);
                    case LETSENCRYPT:
                        return new SSLCertificateCreateFromLetsEncryptProcessor(this);
                    case TE:
                        return new TeCreateProcessor<>(this);
                }
                break;
            case UPDATE:
                switch (context.getEventProvider()) {
                    case PM:
                        return new SSLCertificateUpdateFromPm(this);
                    case LETSENCRYPT:
                        return new SSLCertificateUpdateFromLetsEncrypt(this);
                    case TE:
                        return new TeUpdateProcessor<>(this);
                }
                break;
            case DELETE:
                switch (context.getEventProvider()) {
                    case PM:
                        return new SSLCertificateDeletePmProcessor(this);
                    case TE:
                        return new TeDeleteProcessor<>(this);
                }
                break;
        }
        return null;
    }

    @Override
    public String getResourceType() {
        return Constants.Exchanges.Resource.SSL_CERTIFICATE;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + SSL_CERTIFICATE_CREATE)
    public void handleCreateEvent(
            @Header(value = "provider", required = false) String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleCreate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + SSL_CERTIFICATE_UPDATE)
    public void handleUpdateEvent(
            @Header(value = "provider", required = false) String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleUpdate(eventProvider, serviceMessage);
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + SSL_CERTIFICATE_DELETE)
    public void handleDeleteEvent(
            @Header(value = "provider", required = false) String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        handleDelete(eventProvider, serviceMessage);
    }

    @Override
    protected ServiceMessage createReportMessage(ResourceActionContext<SSLCertificate> context) {
        ServiceMessage event = context.getMessage();

        ServiceMessage report = super.createReportMessage(context);

        if (event.getParams().containsKey("isSafeBrowsing")) {
            report.addParam("isSafeBrowsing", event.getParam("isSafeBrowsing"));
        }

        return report;
    }

    @Override
    protected String getRoutingKey(ResourceActionContext<SSLCertificate> context) {
        String teRoutingKey;
        switch (context.getEventProvider()) {
            case PM:
            case LETSENCRYPT:
                teRoutingKey = ((GovernorOfSSLCertificate) governor).getTERoutingKey(getResourceFromOvsContext(context));
                return teRoutingKey != null ? teRoutingKey : getDefaultRoutingKey();

            case TE:
            default:
                return getDefaultRoutingKey();
        }
    }
}

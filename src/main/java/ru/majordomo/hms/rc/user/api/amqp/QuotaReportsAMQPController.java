package ru.majordomo.hms.rc.user.api.amqp;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.rc.user.api.message.ServiceMessage;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfMailbox;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.DATABASE_QUOTA;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.MAILBOX_QUOTA;
import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.UNIX_ACCOUNT_QUOTA;

@Service
public class QuotaReportsAMQPController {
    private final GovernorOfUnixAccount governorOfUnixAccount;
    private final GovernorOfDatabase governorOfDatabase;
    private final GovernorOfMailbox governorOfMailbox;

    public QuotaReportsAMQPController(
            GovernorOfUnixAccount governorOfUnixAccount,
            GovernorOfDatabase governorOfDatabase,
            GovernorOfMailbox governorOfMailbox
    ) {
        this.governorOfUnixAccount = governorOfUnixAccount;
        this.governorOfDatabase = governorOfDatabase;
        this.governorOfMailbox = governorOfMailbox;
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + UNIX_ACCOUNT_QUOTA)
    public void handleUnixAccountQuotaReport(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        try {
            governorOfUnixAccount.processQuotaReport(serviceMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + DATABASE_QUOTA)
    public void handleDatabaseQuotaReport(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        try {
            governorOfDatabase.processQuotaReport(serviceMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = "${hms.instance.name}" + "." + "${spring.application.name}" + "." + MAILBOX_QUOTA)
    public void handleMailboxQuotaReport(
            Message amqpMessage,
            @Header(value = "provider") String eventProvider,
            @Payload ServiceMessage serviceMessage
    ) {
        try {
            governorOfMailbox.processQuotaReport(serviceMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

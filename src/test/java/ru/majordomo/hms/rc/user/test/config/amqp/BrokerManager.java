package ru.majordomo.hms.rc.user.test.config.amqp;

import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;

public class BrokerManager {

        private static final Broker broker = new Broker();

        public void start() throws Exception {
            String configFileName = this.getClass().getResource("/qpid-config.json").toURI().toString();
            String keyStorePath = this.getClass().getResource("/clientkeystore").toURI().toString();
            BrokerOptions brokerOptions = new BrokerOptions();
            brokerOptions.setConfigProperty("qpid.amqp_port", String.valueOf(AMQPBrokerConfig.BROKER_PORT));
            brokerOptions.setConfigProperty("qpid.keystore_path", keyStorePath);
            brokerOptions.setInitialConfigurationLocation(configFileName);
            broker.startup(brokerOptions);
        }

        public void stop() {
            broker.shutdown();
        }
}

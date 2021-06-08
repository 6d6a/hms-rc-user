package ru.majordomo.hms.rc.user.test.config.amqp;

import com.rabbitmq.client.ConnectionFactory;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.SocketUtils;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.rc.user.api.clients.Sender;

import static ru.majordomo.hms.rc.user.common.Constants.Exchanges.ALL_EXCHANGES;
import static ru.majordomo.hms.rc.user.common.Constants.RC_USER_ROUT;

@Configuration
@EnableRabbit
@Profile("test")
public class AMQPBrokerConfig implements RabbitListenerConfigurer {

    public static final int BROKER_PORT = SocketUtils.findAvailableTcpPort();

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${hms.instance.name}")
    private String instanceName;

    @Bean
    public CachingConnectionFactory connectionFactory() throws Exception {
        ConnectionFactory rabbitConnectionFactory = new ConnectionFactory();
        rabbitConnectionFactory.setUsername("guest");
        rabbitConnectionFactory.setPassword("guest");
        rabbitConnectionFactory.useSslProtocol();
        rabbitConnectionFactory.setVirtualHost("default");
        rabbitConnectionFactory.setHost("localhost");
        rabbitConnectionFactory.setPort(BROKER_PORT);
        rabbitConnectionFactory.setAutomaticRecoveryEnabled(true);

//        return new CachingConnectionFactory(rabbitConnectionFactory);
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(rabbitConnectionFactory);
        cachingConnectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        return cachingConnectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() throws Exception {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        rabbitAdmin.setAutoStartup(false);
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() throws Exception {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setReceiveTimeout(100);
        return new RabbitTemplate(connectionFactory());
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(myHandlerMethodFactory());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() throws Exception {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }

    @Bean
    public DefaultMessageHandlerMethodFactory myHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setMessageConverter(mappingJackson2MessageConverter());
        return factory;
    }

    @Bean
    public MappingJackson2MessageConverter mappingJackson2MessageConverter() {
        return new MappingJackson2MessageConverter();
    }

    @Bean
    RetryOperationsInterceptor interceptor() throws Exception {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate(), "rc-user", "error"))
                .build();
    }

    @Bean
    public Sender sender() throws Exception {
        return new Sender(rabbitTemplate(), applicationName, instanceName);
    }

    @Bean
    public List<Exchange> exchanges() {
        List<Exchange> exchanges = new ArrayList<>();

        for (String exchangeName : ALL_EXCHANGES) {
            exchanges.add(new TopicExchange(exchangeName));
        }

        return exchanges;
    }

    @Bean
    public List<Queue> queues() {
        List<Queue> queues = new ArrayList<>();

        for (String exchangeName : ALL_EXCHANGES) {
            queues.add(new Queue(instanceName + "." + applicationName + "." + exchangeName));
        }

        return queues;
    }

    @Bean
    public List<Binding> bindings() {
        List<Binding> bindings = new ArrayList<>();

        for (String exchangeName : ALL_EXCHANGES) {
            bindings.add(new Binding(
                    instanceName + "." + applicationName + "." + exchangeName,
                    Binding.DestinationType.QUEUE,
                    exchangeName,
                    //instanceName + "." + applicationName, В остальных приложениях это так работает,
                    // но в rcUser джигурдец - (appName = rc-user, routKey = rc.user)
                    instanceName + "." + RC_USER_ROUT,
                    null
            ));
        }

        return bindings;
    }
}

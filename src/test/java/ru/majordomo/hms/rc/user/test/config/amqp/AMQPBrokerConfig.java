package ru.majordomo.hms.rc.user.test.config.amqp;

import com.rabbitmq.client.ConnectionFactory;

import org.springframework.amqp.core.AmqpAdmin;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.SocketUtils;

import ru.majordomo.hms.rc.user.api.clients.Sender;

@Configuration
@EnableRabbit
@Profile("test")
public class AMQPBrokerConfig implements RabbitListenerConfigurer {

    public static final int BROKER_PORT = SocketUtils.findAvailableTcpPort();

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
    public Sender sender() {
        return new Sender();
    }

}

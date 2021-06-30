package ru.majordomo.hms.rc.user.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.SocketUtils;

@Configuration
@EnableRedisRepositories(basePackages = {"ru.majordomo.hms.rc.user.repositoriesRedis"})
@Profile({"default","prod","dev"})
public class RedisConfig {

    private Integer redisPort = SocketUtils.findAvailableTcpPort();
    private String redisHost;

    @Value("${spring.redis.port}")
    public void setRedisPort(Integer redisPort) {
        this.redisPort = redisPort;
    }

    @Value("${spring.redis.host}")
    public void setRedisHost(String redisHost) {
        this.redisHost = redisHost;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }

    @Bean
    public StringRedisSerializer stringRedisSerializer() {
        return new StringRedisSerializer();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setDefaultSerializer(stringRedisSerializer());
        return template;
    }
}

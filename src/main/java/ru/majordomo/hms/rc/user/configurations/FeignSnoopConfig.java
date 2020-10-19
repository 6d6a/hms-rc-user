package ru.majordomo.hms.rc.user.configurations;

import feign.Request;
import feign.auth.BasicAuthRequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@RequiredArgsConstructor
public class FeignSnoopConfig {
    @Value("${snoop.username}")
    private final String username;

    @Value("${snoop.password}")
    private final String password;

    @Value("${snoop.connectTimeout:60000}")
    private int connectTimeout;

    @Value("${snoop.readTimeOut:120000}")
    private int readTimeout;

    @Bean
    public Request.Options options() {
        return new Request.Options(connectTimeout, readTimeout);
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(username, password);
    }
}

package ru.majordomo.hms.rc.user.configurations;

import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.web.context.request.RequestContextListener;

import java.util.Collections;

import feign.Request;
import feign.RequestInterceptor;
import ru.majordomo.hms.personmgr.exception.handler.MajordomoFeignErrorDecoder;
import ru.majordomo.hms.rc.user.security.OAuth2FeignRequestInterceptor;

public class FeignConfig {
    @Value("${security.oauth2.client.accessTokenUri}")
    private String accessTokenUri;

    @Value("${security.oauth2.client.clientId}")
    private String clientId;

    @Value("${security.oauth2.client.clientSecret}")
    private String clientSecret;

    @Value("${security.oauth2.client.scope}")
    private String scope;

    @Value("${si_oauth.serviceUsername}")
    private String username;

    @Value("${si_oauth.servicePassword}")
    private String password;

    @Value("${service.feign.connectTimeout:30000}")
    private int connectTimeout;

    @Value("${service.feign.readTimeOut:120000}")
    private int readTimeout;

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor(){
        AccessTokenRequest accessTokenRequest = new DefaultAccessTokenRequest();
        OAuth2ClientContext oAuth2ClientContext = new DefaultOAuth2ClientContext(accessTokenRequest);

        return new OAuth2FeignRequestInterceptor(oAuth2ClientContext, resource());
    }

    private OAuth2ProtectedResourceDetails resource() {
        ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
        details.setAccessTokenUri(accessTokenUri);
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        details.setScope(Collections.singletonList(scope));
        details.setGrantType("password");
        details.setUsername(username);
        details.setPassword(password);

        return details;
    }

    @Bean
    public RequestContextListener requestContextListener(){
        return new RequestContextListener();
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(connectTimeout, readTimeout);
    }

    @Bean public ErrorDecoder errorDecoder() {
        return new MajordomoFeignErrorDecoder();
    }
}

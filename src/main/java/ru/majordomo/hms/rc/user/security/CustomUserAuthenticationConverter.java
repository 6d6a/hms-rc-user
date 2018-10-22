package ru.majordomo.hms.rc.user.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.api.DTO.UserAccount;

@Slf4j
public class CustomUserAuthenticationConverter extends DefaultUserAuthenticationConverter {
    private static final String ACCOUNT_ID = "accountId";

    @Override
    public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(USERNAME)) {
            String userName = (String) map.get(USERNAME);
            String accountId = (String) map.get(ACCOUNT_ID);

            Collection<? extends GrantedAuthority> authorities = getAuthorities(map);

            UserAccount principal;
            try {
                principal = new UserAccount(
                        userName,
                        accountId,
                        "",
                        true,
                        true,
                        true,
                        true,
                        authorities
                );
            } catch (Exception e) {
                log.error("Got exception when creating UserAccount from map: " + map);
                return null;
            }

            return new UsernamePasswordAuthenticationToken(principal, "N/A", authorities);
        }
        return null;
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
        if (!map.containsKey(AUTHORITIES)) {
            return null;
        }
        Object authorities = map.get(AUTHORITIES);
        if (authorities instanceof String) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList((String) authorities);
        }
        if (authorities instanceof Collection) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList(
                    StringUtils.collectionToCommaDelimitedString((Collection<?>) authorities));
        }
        throw new IllegalArgumentException("Authorities must be either a String or a Collection");
    }
}

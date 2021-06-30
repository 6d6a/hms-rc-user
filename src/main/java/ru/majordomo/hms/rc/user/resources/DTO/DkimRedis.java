package ru.majordomo.hms.rc.user.resources.DTO;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.IDN;

@Data
@RedisHash("dkim")
public class DkimRedis {
    @Id
    @Nullable
    private String id;

    @Nullable
    private String dkimSelector;
    /**
     * приватный ключ которым подписываются письма. Одна большая строка в формате:
     * -----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAnVjJ5X1M7WTNaAuLT294NPFu29msuJ3aj1xzsCbYyVfVxwSL\nnP ... lPquQ==\n-----END RSA PRIVATE KEY-----\n
     */
    @Nullable
    private String dkimKey;

    public static String getRedisId(@Nonnull String domainNameUnicode) {
        return IDN.toASCII(domainNameUnicode);
    }
}

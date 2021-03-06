package ru.majordomo.hms.rc.user.common;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.resources.DKIM;
import sun.security.provider.Sun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@ParametersAreNullableByDefault
public class DKIMManager {
    private static volatile KeyPairGenerator keyGenerator;

    private static synchronized void initKeyGenerator() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGeneratorTemp = KeyPairGenerator.getInstance("RSA");
        SecureRandom random;
        try {
            random = SecureRandom.getInstance("NativePRNG");
            log.debug("Created NativePRNG random generator");
        } catch (NoSuchAlgorithmException noSuchNativePRNGException) {
            log.error("DKIMManager cannot create NativePRNG secure random generator", noSuchNativePRNGException);
            try {
                random = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException noSuchSHA1PRNGException) {
                log.error("DKIMManager cannot create SHA1PRNG secure random generator", noSuchSHA1PRNGException);
                random = new SecureRandom();
            }
        }
        keyGeneratorTemp.initialize(2048, random);
        keyGenerator = keyGeneratorTemp;

    }

    public static DKIM generateDkim(@Nonnull String selector, String dkimContentPattern, @Nonnull String domainId) throws NoSuchAlgorithmException {
        if (keyGenerator == null) {
            initKeyGenerator();
        }

        java.security.KeyPair jsKeyPair = keyGenerator.generateKeyPair();
        String privateKeyBase64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(jsKeyPair.getPrivate().getEncoded());
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n";
        privateKeyPem += privateKeyBase64;
        privateKeyPem += "\n-----END PRIVATE KEY-----\n";
        String publicKeyBase64 = Base64.getMimeEncoder(-1, new byte[]{})
                .encodeToString(jsKeyPair.getPublic().getEncoded());

        DKIM result = new DKIM();
        result.setSelector(selector);
        result.setPrivateKey(privateKeyPem);
        result.setPublicKey(publicKeyBase64);
        result.setSwitchedOn(true);
        result.setData(makeContent(publicKeyBase64, dkimContentPattern));
        result.setId(domainId);
        return result;
    }

    @Nullable
    public static String makeContent(String publicKey, String dkimContentPattern) {
        if (publicKey == null || dkimContentPattern == null) return null;
        return dkimContentPattern.replace("$PUBLICKEY", publicKey);
    }
}

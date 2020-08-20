package ru.majordomo.hms.rc.user.common;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import ru.majordomo.hms.rc.user.resources.DKIM;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@ParametersAreNullableByDefault
public class DKIMManager {
    public static DKIM generateDkim(@Nonnull String selector, String dkimContentPattern) throws JSchException {
        ByteArrayOutputStream privateKey = new ByteArrayOutputStream();
        KeyPair keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, 2048);
        keyPair.writePrivateKey(privateKey);
        byte[] publicBlob = keyPair.getPublicKeyBlob();
        String publicStr = Base64.getEncoder().encodeToString(publicBlob);
        DKIM result = new DKIM();
        result.setSelector(selector);
        result.setPrivateKey(privateKey.toString());
        result.setPublicKey(publicStr);
        result.setSwitchedOn(true);
        result.setData(makeContent(publicStr, dkimContentPattern));
        return result;
    }

    @Nullable
    public static String makeContent(String publicKey, String dkimContentPattern) {
        if (publicKey == null || dkimContentPattern == null) return null;
        return dkimContentPattern.replace("$PUBLICKEY", publicKey);
    }
}

package ru.majordomo.hms.rc.user.common;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import ru.majordomo.hms.rc.user.resources.SSHKeyPair;

public class SSHKeyManager {
    public static SSHKeyPair generateKeyPair() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        SSHKeyPair sshKeyPair = new SSHKeyPair();

        Base64.Encoder encoder = Base64.getMimeEncoder();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        String privateKeyAsString = "-----BEGIN RSA PRIVATE KEY-----\n" +
                new String(encoder.encode(privateKey.getEncoded()), "UTF-8") +
                "\n-----END RSA PRIVATE KEY-----";

        String publicKeyAsString = "ssh-rsa " +
                new String(encoder.encode(publicKey.getEncoded()), "UTF-8") + "\n";

        sshKeyPair.setPrivateKey(privateKeyAsString);
        sshKeyPair.setPublicKey(publicKeyAsString);

        return sshKeyPair;
    }

}

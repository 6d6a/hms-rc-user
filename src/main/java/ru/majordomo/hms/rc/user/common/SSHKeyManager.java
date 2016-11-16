package ru.majordomo.hms.rc.user.common;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import ru.majordomo.hms.rc.user.resources.SSHKeyPair;

public class SSHKeyManager {
    public static SSHKeyPair generateKeyPair() throws JSchException {
        ByteArrayOutputStream privateKey = new ByteArrayOutputStream();
        ByteArrayOutputStream publicKey = new ByteArrayOutputStream();

        JSch jSch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.DSA);
        keyPair.writePrivateKey(privateKey);
        keyPair.writePublicKey(publicKey, "");

        SSHKeyPair sshKeyPair = new SSHKeyPair();
        sshKeyPair.setPrivateKey(privateKey.toString());
        sshKeyPair.setPublicKey(publicKey.toString());

        return sshKeyPair;
    }

}

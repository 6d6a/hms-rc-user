package ru.majordomo.hms.rc.user.test.external;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JSchTest {
    @Test
    public void generateKeyPair() throws Exception {
        ByteArrayOutputStream privateKey = new ByteArrayOutputStream();
        ByteArrayOutputStream publicKey = new ByteArrayOutputStream();
        String publicKeyComment = "Публичный ключ для аккаунта ac_100000";

        JSch jSch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jSch, KeyPair.DSA);

        keyPair.writePrivateKey(privateKey);
        keyPair.writePublicKey(publicKey, publicKeyComment);

        String[] privateKeyLines = privateKey.toString().split("\n");
        String[] publicKeyParts = publicKey.toString().split(" ");
        int privateKeyLinesCount = privateKeyLines.length;

        assertThat(privateKeyLinesCount, is(12));
        assertThat(privateKeyLines[0], is("-----BEGIN DSA PRIVATE KEY-----"));
        assertThat(privateKeyLines[privateKeyLinesCount-1], is("-----END DSA PRIVATE KEY-----"));

        assertThat(publicKeyParts[0], is("ssh-dss"));
    }
}

package ru.majordomo.hms.rc.user.common;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.rc.user.resources.SSHKeyPair;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@Slf4j
@ParametersAreNonnullByDefault
public class SSHKeyManager {
    public static SSHKeyPair generateKeyPair() throws JSchException {
        ByteArrayOutputStream privateKey = new ByteArrayOutputStream();
        ByteArrayOutputStream publicKey = new ByteArrayOutputStream();

        KeyPair keyPair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, 2048);
        keyPair.writePrivateKey(privateKey);
        keyPair.writePublicKey(publicKey, "");

        SSHKeyPair sshKeyPair = new SSHKeyPair();
        sshKeyPair.setPrivateKey(privateKey.toString());
        sshKeyPair.setPublicKey(publicKey.toString());

        return sshKeyPair;
    }

    @Nullable
    public static byte[] convertPemToPpk(String privateKeyPem, String puttygenPath) throws InterruptedException, IOException {
        Path pemFile = null;
        Path ppkFile = null;
        try {
            pemFile = Files.createTempFile("rc-user_pem2ppk", "");
            Files.write(pemFile, Collections.singletonList(privateKeyPem));
            String ppkName = pemFile.toAbsolutePath().toString() + ".ppk";
            Process puttygen = new ProcessBuilder().command(puttygenPath, pemFile.toAbsolutePath().toString(), "-o", ppkName).start();
            int exitCode = puttygen.waitFor();
            ppkFile = Paths.get(ppkName);
            if (exitCode == 0) {
                return Files.readAllBytes(ppkFile);
            } else {
                String errorLines = new BufferedReader(new InputStreamReader(puttygen.getErrorStream())).lines().collect(Collectors.joining(String.format("%n")));
                log.error(String.format("puttygen returned error code: %d. Error output: %n%s%n", exitCode, errorLines));
                return null;
            }
        } finally {
            if (ppkFile != null) {
                Files.deleteIfExists(ppkFile);
            }
            if (pemFile != null) {
                Files.deleteIfExists(pemFile);
            }
        }
    }
}

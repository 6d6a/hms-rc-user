package ru.majordomo.hms.rc.user.common;

import java.io.UnsupportedEncodingException;

import static org.apache.commons.codec.digest.Crypt.crypt;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;

public class PasswordManager {
    public static String forFtp(String plainPassword) throws UnsupportedEncodingException {
        byte[] password = plainPassword.getBytes("UTF-8");
        return sha1Hex(md5Hex(sha1Hex(password)));
    }

    public static String forMySQL5(String plainPassword) throws UnsupportedEncodingException {
        byte[] password = plainPassword.getBytes("UTF-8");

        return "*" + sha1Hex(sha1(password)).toUpperCase();
    }

    public static String forUbuntu(String plainPassword) throws UnsupportedEncodingException {
        byte[] password = plainPassword.getBytes("UTF-8");
        String salt = "$6$" + randomAlphabetic(8);

        return crypt(password, salt);
    }

    public static String forWeb(String plainPassword) throws UnsupportedEncodingException {
        byte[] password = plainPassword.getBytes("UTF-8");
        return sha1Hex(md5Hex(sha1Hex(password)));
    }

    public static String forPostgres(String plainPassword) throws UnsupportedEncodingException {
        byte[] password = plainPassword.getBytes("UTF-8");
        return md5Hex(password);
    }

    public static String generatePlainPassword() {
        return randomAlphabetic(8);
    }
}



package ru.majordomo.hms.rc.user.test.common;

import org.junit.Test;

import ru.majordomo.hms.rc.user.common.SSHKeyManager;

public class SSHKeyManagerTest {

    @Test
    public void generateKeyPair() throws Exception{
        System.out.println(SSHKeyManager.generateKeyPair());
    }


}
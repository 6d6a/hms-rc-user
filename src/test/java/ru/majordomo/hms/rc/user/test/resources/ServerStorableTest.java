package ru.majordomo.hms.rc.user.test.resources;

import org.junit.Test;

import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ServerStorable;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import static org.junit.Assert.assertTrue;

public class ServerStorableTest {
    @Test
    public void checkIsServerStorable() {
        Resource resource = new UnixAccount();
        assertTrue(resource instanceof ServerStorable);
    }
}

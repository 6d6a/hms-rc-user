package ru.majordomo.hms.rc.user.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class PathManagerTest {
    @Test
    public void isPathInsideTheDirNotValid() throws Exception {
        String unixAccountHomeDir = "/home/u2000";

        String webSiteDir = "asdasdasdasd.com/www/../../../123";

        assertFalse(PathManager.isPathInsideTheDir(webSiteDir, unixAccountHomeDir));
    }

    @Test
    public void isPathInsideTheDirValid() throws Exception {
        String unixAccountHomeDir = "/home/u2000";

        String webSiteDir = "asdasdasdasd.com/www";

        assertTrue(PathManager.isPathInsideTheDir(webSiteDir, unixAccountHomeDir));
    }

    @Test
    public void isPathInsideTheDirWithDotsValid() throws Exception {
        String unixAccountHomeDir = "/home/u2000";

        String webSiteDir = "asdasdasdasd.com/www/../../123";

        assertTrue(PathManager.isPathInsideTheDir(webSiteDir, unixAccountHomeDir));
    }
}
package ru.majordomo.hms.rc.user.common;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathManager {
    public static Boolean isPathInsideTheDir(String path, String dir) {
        Path unixAccountHomeDir = Paths.get(dir);

        Path webSiteDir = unixAccountHomeDir.resolve(path);

        return webSiteDir.normalize().startsWith(unixAccountHomeDir);
    }
}

package ru.majordomo.hms.rc.user.resources;

import java.util.HashMap;
import java.util.Map;

public enum ResourceArchiveType {
    WEBSITE,
    DATABASE;

    public static final Map<ResourceArchiveType, String> FILE_EXTENSION;
    public static final String DEFAULT_FILE_EXTENSION = "tar.gz";

    static {
        FILE_EXTENSION = new HashMap<>();

        FILE_EXTENSION.put(WEBSITE, DEFAULT_FILE_EXTENSION);
        FILE_EXTENSION.put(DATABASE, "sql.gz");
    }
}

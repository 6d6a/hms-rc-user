package ru.majordomo.hms.rc.user.importing;

public interface ResourceDBImportService {
    void pull(String accountId, String serverId);
    boolean importToMongo(String accountId, String serverId);
}

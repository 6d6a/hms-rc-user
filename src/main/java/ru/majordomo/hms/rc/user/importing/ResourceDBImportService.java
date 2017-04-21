package ru.majordomo.hms.rc.user.importing;

public interface ResourceDBImportService {
    void pull();
    void pull(String accountId);
    boolean importToMongo();
    boolean importToMongo(String accountId);
}

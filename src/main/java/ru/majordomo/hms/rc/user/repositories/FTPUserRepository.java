package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.FTPUser;

import java.util.List;

public interface FTPUserRepository extends MongoRepository<FTPUser, String> {
    List<FTPUser> findByAccountId(String accountId);
    List<FTPUser> findByUnixAccountId(String unixAccountId);
    FTPUser findOneByName(String name);
    FTPUser findByIdAndAccountId(String ftpUserId, String accountId);
    Long countByAccountId(String accountId);
    FTPUser findByNameAndAccountId(String name, String accountId);
}

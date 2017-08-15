package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.util.List;

public interface UnixAccountRepository extends MongoRepository<UnixAccount,String> {
    UnixAccount findFirstByOrderByUidDesc();
    UnixAccount findFirstByOrderByUidAsc();
    UnixAccount findByUid(Integer uid);
    Page<UnixAccount> findAllByOrderByUidAsc(Pageable pageable);
    List<UnixAccount> findByAccountId(String accountId);
    List<UnixAccount> findByServerId(String serverId);
    UnixAccount findByIdAndAccountId(String unixAccountId, String accountId);
}

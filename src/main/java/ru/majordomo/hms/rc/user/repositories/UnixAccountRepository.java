package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.UnixAccount;

public interface UnixAccountRepository extends MongoRepository<UnixAccount,String> {
    UnixAccount findFirstByOrderByUidDesc();
    UnixAccount findFirstByOrderByUidAsc();
    Page<UnixAccount> findAllByOrderByUidAsc(Pageable pageable);
}

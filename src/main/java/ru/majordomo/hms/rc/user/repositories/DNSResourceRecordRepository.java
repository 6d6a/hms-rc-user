package ru.majordomo.hms.rc.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.rc.user.resources.DNSResourceRecord;

public interface DNSResourceRecordRepository extends MongoRepository<DNSResourceRecord,String> {

}

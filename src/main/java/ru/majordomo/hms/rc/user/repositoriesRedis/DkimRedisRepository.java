package ru.majordomo.hms.rc.user.repositoriesRedis;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.majordomo.hms.rc.user.resources.DTO.DkimRedis;

@Repository
public interface DkimRedisRepository extends CrudRepository<DkimRedis, String> {
}

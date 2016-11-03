package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.managers.GovernorOfPerson;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.Resource;

@RestController
public class DatabaseUserRestController {

    private GovernorOfDatabaseUser governor;

    @Autowired
    public void setGovernor(GovernorOfDatabaseUser governor) {
        this.governor = governor;
    }

    @RequestMapping(value = {"/database-user/{databaseUserId}", "/database-user/{databaseUserId}/"}, method = RequestMethod.GET)
    public DatabaseUser readOne(@PathVariable String databaseUserId) {
        return (DatabaseUser) governor.build(databaseUserId);
    }

    @RequestMapping(value = {"/database-user/","/database-user"}, method = RequestMethod.GET)
    public Collection<? extends Resource> readAll() {
        return governor.buildAll();
    }

}

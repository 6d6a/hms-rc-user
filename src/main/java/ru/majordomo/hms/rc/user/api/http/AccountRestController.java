package ru.majordomo.hms.rc.user.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.api.interfaces.StaffResourceControllerClient;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabase;
import ru.majordomo.hms.rc.user.managers.GovernorOfDatabaseUser;
import ru.majordomo.hms.rc.user.managers.GovernorOfUnixAccount;
import ru.majordomo.hms.rc.user.managers.GovernorOfWebSite;
import ru.majordomo.hms.rc.user.resources.*;

import java.util.*;

@RestController
public class AccountRestController {

    private StaffResourceControllerClient staffResourceControllerClient;
    private GovernorOfUnixAccount governorOfUnixAccount;
    private GovernorOfDatabase governorOfDatabase;
    private GovernorOfDatabaseUser governorOfDatabaseUser;
    private GovernorOfWebSite governorOfWebSite;

    @Autowired
    public AccountRestController(
            StaffResourceControllerClient staffResourceControllerClient,
            GovernorOfUnixAccount governorOfUnixAccount,
            GovernorOfDatabase governorOfDatabase,
            GovernorOfDatabaseUser governorOfDatabaseUser,
            GovernorOfWebSite governorOfWebSite
    ) {
        this.staffResourceControllerClient = staffResourceControllerClient;
        this.governorOfUnixAccount = governorOfUnixAccount;
        this.governorOfDatabase = governorOfDatabase;
        this.governorOfDatabaseUser = governorOfDatabaseUser;
        this.governorOfWebSite = governorOfWebSite;
    }

    @PreAuthorize("hasAuthority('TRANSFER_ACCOUNT')")
    @PostMapping("/{accountId}/account-move")
    public Boolean moveAccount(
            @PathVariable("accountId") String accountId,
            @RequestBody Map<String, String> params
    ) {
        String desiredServerId = params.get("serverId");
        Server desiredServer = staffResourceControllerClient.getServerById(desiredServerId);
        if (desiredServer == null) {
            throw new ParameterValidationException("Не найден сервер " + desiredServerId);
        }
        List<Service> desiredServerServices = desiredServer.getServices();

        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);
        Collection<UnixAccount> unixAccounts = governorOfUnixAccount.buildAll(keyValue);
        UnixAccount unixAccount = null;
        if (unixAccounts.iterator().hasNext()) {
            unixAccount = unixAccounts.iterator().next();
        }
        if (unixAccount == null) {
            throw new ParameterValidationException("Не найден UnixAccount для аккаунта " + accountId);
        }

        String currentServerId = unixAccount.getServerId();
        Server currentServer = staffResourceControllerClient.getServerById(currentServerId);
        List<Service> currentServerServices = currentServer.getServices();

        unixAccount.setServerId(desiredServerId);

        List<Serviceable> serviceableResources = getServiceableResources(accountId);
        for (Serviceable serviceable : serviceableResources) {
            String currentServiceId = serviceable.getServiceId();
            String currentServiceName = currentServerServices.stream()
                    .filter(s -> s.getId().equals(currentServiceId))
                    .findFirst().orElse(new Service()).getName();
            if (currentServiceName == null) {
                throw new ParameterValidationException("Не вышло");
            }

            String template = currentServiceName.split("@")[0];

            String desiredServiceId = desiredServerServices.stream()
                    .filter(s -> s.getName().split("@")[0].equals(template))
                    .findFirst().orElse(new Service()).getId();
            if (desiredServiceId == null) {
                throw new ParameterValidationException("Не вышло");
            }

            serviceable.setServiceId(desiredServiceId);
        }

        governorOfUnixAccount.store(unixAccount);
        saveServicable(serviceableResources);

        return true;
    }

    private List<Serviceable> getServiceableResources(String accountId) {
        List<Serviceable> resources = new ArrayList<>();
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put("accountId", accountId);

        Collection<WebSite> webSites = governorOfWebSite.buildAll(keyValue);
        resources.addAll(webSites);

        Collection<Database> databases = governorOfDatabase.buildAll(keyValue);
        resources.addAll(databases);

        Collection<DatabaseUser> databaseUsers = governorOfDatabaseUser.buildAll(keyValue);
        resources.addAll(databaseUsers);

        return resources;
    }

    private void saveServicable(List<Serviceable> serviceableResources) {
        for (Serviceable serviceable : serviceableResources) {
            if (serviceable instanceof Database) {
                governorOfDatabase.store((Database) serviceable);
            }
            if (serviceable instanceof DatabaseUser) {
                governorOfDatabaseUser.store((DatabaseUser) serviceable);
            }
            if (serviceable instanceof WebSite) {
                governorOfWebSite.store((WebSite) serviceable);
            }
        }
    }
}

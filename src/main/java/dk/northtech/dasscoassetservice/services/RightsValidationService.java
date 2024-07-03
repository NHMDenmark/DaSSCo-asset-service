package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class RightsValidationService {
    InstitutionService institutionService;
    CollectionService collectionService;
    private Jdbi jdbi;

    public static final String WRITE_ROLE_PREFIX = "WRITE_";
    public static final String READ_ROLE_PREFIX = "READ_";

    @Inject
    public RightsValidationService(InstitutionService institutionService, CollectionService collectionService, Jdbi jdbi) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.jdbi = jdbi;
    }

    public boolean checkReadRights(User user, String institutionName, String collectionName) {
        return checkRights(user, institutionName,collectionName, false);
    }

    public boolean checkWriteRights(User user, String institutionName, String collectionName) {
        return checkRights(user, institutionName,collectionName, true);
    }
    public boolean checkRights(User user, String institutionName, String collectionName, boolean write) {
        Set<String> roles = user.roles;
        if (roles.contains(InternalRole.ADMIN.roleName)
            || roles.contains(InternalRole.SERVICE_USER.roleName)
            || roles.contains(InternalRole.DEVELOPER.roleName)) {
            return true;
        }
        Optional<Collection> collecionOpt = collectionService.findCollection(collectionName, institutionName);
        if (collecionOpt.isEmpty()) {
            throw new DasscoIllegalActionException("Collection " + collectionName + " does not exist within institution " + institutionName);
        }
        Set<String> allUserRoles = getUserRoles(user.roles);
        Collection collection = collecionOpt.get();

        if (!collection.roleRestrictions().isEmpty()) {
            for (Role r : collection.roleRestrictions()) {
                System.out.println((write ? WRITE_ROLE_PREFIX: READ_ROLE_PREFIX) + r.name());
                if(allUserRoles.contains((write ? WRITE_ROLE_PREFIX: READ_ROLE_PREFIX) + r.name())){
                    return true;
                }
            }
            return false;
        }
        Optional<Institution> ifExists = institutionService.getIfExists(institutionName);
        if(ifExists.isEmpty()) {
            throw new RuntimeException("This should not happen :^)");
        }
        Institution institution = ifExists.get();
        if (!institution.roleRestriction().isEmpty()) {
            for (Role r : institution.roleRestriction()) {
                if(allUserRoles.contains((write?WRITE_ROLE_PREFIX: READ_ROLE_PREFIX)+r.name())){
                    return true;
                }
            }
            return false;
        }
        //If no roles exists everyone has access
        return true;
    }

    //expand write roles into read roles and writeroles
    public Set<String> getUserRoles(Set<String> roles) {
        Set<String> allRoles = new HashSet<>();
        for (String r : roles) {
            if (r.startsWith(WRITE_ROLE_PREFIX)) {
                allRoles.add(READ_ROLE_PREFIX + r.substring(WRITE_ROLE_PREFIX.length()));
            }
            allRoles.add(r);
        }
        return allRoles;
    }

    public void checkRoles(Set<Role> roles, Set<String> userRoles) {

    }
}

package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.UserRepository;
import dk.northtech.dasscoassetservice.domain.Collection;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.util.Map.entry;

@Service
public class RightsValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RightsValidationService.class);
    InstitutionService institutionService;
    CollectionService collectionService;
    AssetGroupService assetGroupService;
    private Jdbi jdbi;

    public static final String WRITE_ROLE_PREFIX = "WRITE_";
    public static final String READ_ROLE_PREFIX = "READ_";

    @Inject
    public RightsValidationService(InstitutionService institutionService, @Lazy CollectionService collectionService,
                                   @Lazy AssetGroupService assetGroupService, Jdbi jdbi) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.assetGroupService = assetGroupService;
        this.jdbi = jdbi;
    }

    public boolean checkReadRights(User user, String institutionName) {
        return checkRights(user, institutionName, false);
    }

    public void checkReadRightsThrowing(User user, String institutionName) {
        boolean hasRight = checkRights(user, institutionName, false);
        if(!hasRight){
            LOGGER.info("User {} does not have read access to institution {}",user.username, institutionName);
            throw new DasscoIllegalActionException();
        }
    }

    public void checkWriteRightsThrowing(User user, String institutionName) {
        boolean hasRight = checkRights(user, institutionName, false);
        if(!hasRight){
            LOGGER.info("User {} does not have write access to institution {}",user.username, institutionName);
            throw new DasscoIllegalActionException();
        }
    }


    public boolean checkWriteRights(User user, String institutionName) {
        return checkRights(user, institutionName, true);
    }

    public boolean checkRights(User user, String institutionName, boolean write) {
        Set<String> roles = user.roles;
        //Default roles always have rights
        if (roles.contains(InternalRole.ADMIN.roleName)
            || roles.contains(InternalRole.SERVICE_USER.roleName)
            || roles.contains(InternalRole.DEVELOPER.roleName)) {
            return true;
        }
        Set<String> allUserRoles = getUserRoles(user.roles);
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
    public boolean checkReadRights(User user, String institutionName, String collectionName) {
        return checkRights(user, institutionName,collectionName, false);
    }

    public boolean checkWriteRights(User user, String institutionName, String collectionName) {
        return checkRights(user, institutionName,collectionName, true);
    }

    public void checkReadRightsThrowing(User user, String institutionName, String collectionName) {
        boolean hasRight = checkRights(user, institutionName, collectionName, false);
        if(!hasRight) {
            LOGGER.warn("User {} does not have read access to collection {} in institution {}",user.username,collectionName,institutionName);
            throw new DasscoIllegalActionException("FORBIDDEN");
        }
    }

    public void checkReadRightsThrowing(User user, String institutionName, String collectionName, String assetGuid){
        boolean hasRight = checkRights(user, institutionName, collectionName, assetGuid, false);
        if(!hasRight) {
            LOGGER.warn("User {} does not have read access to asset {} in collection {} in institution {}",user.username,assetGuid,collectionName,institutionName);
            throw new DasscoIllegalActionException("FORBIDDEN");
        }
    }

    public boolean checkReadRightsThrowing(User user, AssetGroup assetGroup){
        boolean hasRight = checkRights(user, assetGroup);
        if (!hasRight) {
            LOGGER.warn("User {} does not have read access to assetGroup {}",user.username,assetGroup.group_name);
            throw new DasscoIllegalActionException("FORBIDDEN");
        }
        return true;
    }

    public void checkAssetGroupOwnershipThrowing(User user, AssetGroup assetGroup){
        boolean hasRight = checkAssetGroupOwnership(user, assetGroup);
        if (!hasRight) {
            LOGGER.warn("User {} does not have write access to assetGroup {}",user.username,assetGroup.group_name);
            throw new DasscoIllegalActionException("User is not the owner of this asset group.");
        }
    }

    public void checkWriteRightsThrowing(User user, String institutionName, String collectionName) {
        boolean hasRight = checkRights(user, institutionName, collectionName, true);
        if(!hasRight) {
            LOGGER.warn("User {} does not have write access to collection {} in institution {}",user.username,collectionName,institutionName);
            throw new DasscoIllegalActionException("FORBIDDEN");
        }
    }
    public boolean checkRights(User user, String institutionName, String collectionName, boolean write) {
        Set<String> roles = user.roles;
        if (roles.contains(InternalRole.ADMIN.roleName)
            || roles.contains(InternalRole.SERVICE_USER.roleName)
            || roles.contains(InternalRole.DEVELOPER.roleName)) {
            return true;
        }
        Optional<Collection> collectionOpt = collectionService.findCollectionInternal(collectionName, institutionName);
        if (collectionOpt.isEmpty()) {
            throw new DasscoIllegalActionException("Collection " + collectionName + " does not exist within institution " + institutionName);
        }
        Set<String> allUserRoles = getUserRoles(user.roles);
        Collection collection = collectionOpt.get();
        if (!collection.roleRestrictions().isEmpty()) {
            for (Role r : collection.roleRestrictions()) {
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

    public boolean checkRights(User user, String institutionName, String collectionName, String assetGuid, boolean write){
        Set<String> roles = user.roles;
        if (roles.contains(InternalRole.ADMIN.roleName)
                || roles.contains(InternalRole.SERVICE_USER.roleName)
                || roles.contains(InternalRole.DEVELOPER.roleName)) {
            return true;
        }

        boolean hasAccess = jdbi.onDemand(UserRepository.class).userHasAccessToAsset(user.username, assetGuid);
        if (hasAccess){
            return true;
        }

        Optional<Collection> collectionOpt = collectionService.findCollectionInternal(collectionName, institutionName);
        if (collectionOpt.isEmpty()) {
            throw new DasscoIllegalActionException("Collection " + collectionName + " does not exist within institution " + institutionName);
        }
        Set<String> allUserRoles = getUserRoles(user.roles);
        Collection collection = collectionOpt.get();
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

    public boolean checkRights(User user, AssetGroup assetGroup){
        Set<String> roles = user.roles;
        if (roles.contains(InternalRole.ADMIN.roleName)
                || roles.contains(InternalRole.SERVICE_USER.roleName)
                || roles.contains(InternalRole.DEVELOPER.roleName)) {
            return true;
        }

        if (assetGroup.hasAccess.contains(user.username)){
            return true;
        }

        return false;
    }

    public boolean checkAssetGroupOwnership(User user, AssetGroup assetGroup){
        Set<String> roles = user.roles;
        if (roles.contains(InternalRole.ADMIN.roleName)
                || roles.contains(InternalRole.SERVICE_USER.roleName)
                || roles.contains(InternalRole.DEVELOPER.roleName)) {
            return true;
        }

        return jdbi.onDemand(UserRepository.class).isUserOwnerOfAssetGroup(assetGroup.group_name.toLowerCase(), user.username);
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

    public Set<String> getInstitutionsReadRights(Set<String> userRoles) {
        Set<String> institutionNames = new HashSet<>();
        List<Institution> institutionList = this.institutionService.listInstitutions();
        for (Institution institution : institutionList) {
            if (!institution.roleRestriction().isEmpty()) { // institution has role restrictions
                for (Role role : institution.roleRestriction()) {
                    if (userRoles.contains(READ_ROLE_PREFIX + role.name()) || userRoles.contains(WRITE_ROLE_PREFIX + role.name())) {
                        institutionNames.add(institution.name());
                    }
                }
            } else {
                institutionNames.add(institution.name()); // no restrictions, everyone can access yay
            }
        }
        return institutionNames;
    }

    public Map<String, Set<String>> getCollectionRights(Set<String> userRoles) {
        Set<String> collectionReadNames = new HashSet<>();
        Set<String> collectionWriteNames = new HashSet<>();
        List<Collection> collectionList = this.collectionService.getAll();

        for (Collection collection : collectionList) {
            if (!collection.roleRestrictions().isEmpty()) { // collection has role restrictions
                for (Role role : collection.roleRestrictions()) {
                    if (userRoles.contains(READ_ROLE_PREFIX + role.name())) {
                        collectionReadNames.add(collection.name());
                    } else if (userRoles.contains(WRITE_ROLE_PREFIX + role.name())) {
                        collectionWriteNames.add(collection.name());
                    }
                }
            } else { // collection has NO restrictions
                Optional<Institution> institution = this.institutionService.getIfExists(collection.institution());
                if (institution.isPresent()) { // collection is role-less, so we need to check that the user can access the institution
                    if (institution.get().roleRestriction().isEmpty()) { // institute and collection is free-for-all
                        collectionWriteNames.add(collection.name());
                    } else {
                        for (Role role : institution.get().roleRestriction()) { // checking the user can access the institution
                            if (userRoles.contains(READ_ROLE_PREFIX + role.name())) {
                                collectionReadNames.add(collection.name());
                            } else if (userRoles.contains(WRITE_ROLE_PREFIX + role.name())) {
                                collectionWriteNames.add(collection.name());
                            }
                        }
                    }
                }
            }
        }
        return Map.ofEntries(entry("read", collectionReadNames), entry("write", collectionWriteNames));
    }
}

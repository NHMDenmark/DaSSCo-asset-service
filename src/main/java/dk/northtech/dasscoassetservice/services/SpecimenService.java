package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.cache.PreparationTypeCache;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.repositories.RestrictedObjectType;
import dk.northtech.dasscoassetservice.repositories.RoleRepository;
import dk.northtech.dasscoassetservice.repositories.SpecimenRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SpecimenService {
    private final Jdbi jdbi;
    private static final Logger log = LoggerFactory.getLogger(SpecimenService.class);
    private final RoleService roleService;

    private PreparationTypeCache preparationTypeCache;
    private ExtendableEnumService extendableEnumService;
    private RightsValidationService rightsValidationService;
    private CollectionService collectionService;

    @Inject
    public SpecimenService(Jdbi jdbi, PreparationTypeCache preparationTypeCache, ExtendableEnumService extendableEnumService, RightsValidationService rightsValidationService, CollectionService collectionService, RoleService roleService) {
        this.jdbi = jdbi;
        this.preparationTypeCache = preparationTypeCache;
        this.extendableEnumService = extendableEnumService;
        this.rightsValidationService = rightsValidationService;
        this.collectionService = collectionService;
        this.roleService = roleService;
    }

    Optional<Specimen> findSpecimen(Integer specimenId) {
        if (specimenId == null) {
            return Optional.empty();
        }
        return jdbi.withHandle(handle -> {
            RoleRepository roleRepository = handle.attach(RoleRepository.class);
            SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
            Optional<Specimen> specimenOpt = specimenRepository.findSpecimenById(specimenId);
            if (specimenOpt.isEmpty()) {
                return Optional.empty();
            }
            Specimen specimen = specimenOpt.get();
            specimen.role_restrictions().addAll(roleRepository.findRoleRestrictions(RestrictedObjectType.SPECIMEN, specimen.specimen_id()));
            return Optional.of(specimen);
        });
    }

    public Optional<Specimen> findSpecimen(String institution, String collection, String barcode, User user) {
        Optional<Specimen> specimenOpt = findSpecimen(institution, collection, barcode);
        if (specimenOpt.isPresent() && !rightsValidationService.checkRightsSpecimen(user, specimenOpt.get(), false)) {
            throw new DasscoIllegalActionException("FORBIDDEN");
        }
        return specimenOpt;
    }

    public Response deleteSpecimen(String institution, String collection, String barcode, User user) {
        if(user.token == null) {
            throw new DasscoIllegalActionException("FORBIDDEN");
        }
        Optional<Specimen> optionalSpecimen = findSpecimen(institution, collection, barcode);
        if (optionalSpecimen.isEmpty()) {
            throw new NotFoundException("No specimen found with institution " + institution + ", collection " + collection + " and barcode " + barcode);
        }else{
            Specimen specimen = optionalSpecimen.get();
            if(!rightsValidationService.checkRightsSpecimen(user, specimen, true)){
                throw new DasscoIllegalActionException("FORBIDDEN");
            }
            List<String> assetGuids = this.jdbi.withHandle(h -> h.createQuery("select asset_guid from asset_specimen where specimen_id = :specimen_id").bind("specimen_id", specimen.specimen_id).mapTo(String.class).list());
            if(!assetGuids.isEmpty()) {
                return Response.status(Response.Status.FORBIDDEN).entity("Can't delete specimen with institution " + institution + ", collection " + collection + " and barcode " + barcode + ", it has the following assets attached " + assetGuids).build();
            }else{
                return this.jdbi.inTransaction(handle -> {
                    SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
                    specimenRepository.deleteSpecimenRestrictionsWithSpecimenId(specimen.specimen_id());
                    specimenRepository.deleteSpecimenWithCollectionAndBarcode(specimen.collection_id(), barcode);
                    return Response.status(Response.Status.OK).entity("specimen deleted with institution %s, collection %s and barcode %s".formatted(institution, collection, barcode)).build();
                });
            }
        }
    }

    public List<AssetSpecimen> findAssetSpecimens(String asset_guid) {
        SpecimenRepository specimenRepository = jdbi.onDemand(SpecimenRepository.class);
        List<AssetSpecimen> assetSpecimens = specimenRepository.findAssetSpecimens(asset_guid);
        assetSpecimens.forEach(assetSpecimen -> {
            Optional<Specimen> specimen = findSpecimen(assetSpecimen.specimen_id);
            specimen.ifPresent(value -> assetSpecimen.specimen = value);
        });
        return assetSpecimens;
    }

    Optional<Specimen> findSpecimen(String institution, String collection, String barcode) {
        Optional<Collection> collectionInternal = collectionService.findCollectionInternal(collection, institution);
        if (collectionInternal.isEmpty()) {
            return Optional.empty();
        }
        Integer collectionId = collectionInternal.get().collection_id();
        return jdbi.withHandle(handle -> {
            RoleRepository roleRepository = handle.attach(RoleRepository.class);
            SpecimenRepository specimenRepository = handle.attach(SpecimenRepository.class);
            Optional<Specimen> specimenOpt = specimenRepository.findSpecimenByCollectionAndBarcode(collectionId, barcode);
            if (specimenOpt.isEmpty()) {
                return Optional.empty();
            }
            Specimen specimen = specimenOpt.get();
            specimen.role_restrictions().addAll(roleRepository.findRoleRestrictions(RestrictedObjectType.SPECIMEN, specimen.specimen_id()));
            return Optional.of(specimen);
        });
    }

    public Specimen putSpecimen(Specimen specimen, User user) {
        if (Strings.isNullOrEmpty(specimen.specimen_pid())) {
            specimen.specimen_pid = generateDefaultSpecimenPid(specimen.institution(), specimen.collection(), specimen.barcode());
        }
        validateSpecimen(specimen);
        Integer collectionId = getCollectionId(specimen.collection(), specimen.institution());
        return jdbi.inTransaction(h -> {
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);

            Optional<Specimen> existingSpecimen = specimenRepository.findSpecimenByCollectionAndBarcode(collectionId, specimen.barcode());
            if (existingSpecimen.isEmpty()) {
                Specimen specimenWithCollectionId = new Specimen(specimen, null, collectionId);
                Integer specimen_id = specimenRepository.insert_specimen(specimenWithCollectionId);

                if (!specimenWithCollectionId.role_restrictions().isEmpty()) {
                    if(!rightsValidationService.checkRightsSpecimen(user, specimenWithCollectionId, true)) {
                        throw new DasscoIllegalActionException("FORBIDDEN");
                    }
                    RoleRepository roleRepository = h.attach(RoleRepository.class);
                    List<String> roles = roleRepository.listRoles();
                    for(Role role: specimenWithCollectionId.role_restrictions()) {
                        if(!roles.contains(role.name())) {
                            roleRepository.createRole(role.name());
                        }
                    }
                    roleRepository.setRestrictions(RestrictedObjectType.SPECIMEN, specimenWithCollectionId.role_restrictions(), specimen_id);

                }
                return new Specimen(specimenWithCollectionId, specimen_id, specimenWithCollectionId.collection_id());
            } else {
                if(!rightsValidationService.checkRightsSpecimen(user, existingSpecimen.get(), true)){
                    throw new DasscoIllegalActionException("FORBIDDEN");
                }
                return updateSpecimen(specimen, existingSpecimen.get(), user);
            }
        });
    }

    public Specimen updateSpecimen(Specimen specimen, Specimen existing, User user) {
        return jdbi.inTransaction(h -> {
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);

            rightsValidationService.requireWriteRights(user, specimen.institution(), specimen.collection());
            List<String> assetsWithRemovedPreparationType = specimenRepository.getGuidsByPreparationTypeAndSpecimenId(specimen.preparation_types(), existing.specimen_id());
            if (specimen.role_restrictions() != null && !existing.role_restrictions().equals(specimen.role_restrictions())) {
                RoleRepository roleRepository = h.attach(RoleRepository.class);
                List<String> roles = roleRepository.listRoles();
                for(Role role: specimen.role_restrictions()) {
                    if(!roles.contains(role.name())) {
                        roleRepository.createRole(role.name());
                    }
                }
                roleRepository.setRestrictions(RestrictedObjectType.SPECIMEN, specimen.role_restrictions(), existing.specimen_id());
            }
            if (!assetsWithRemovedPreparationType.isEmpty()) {
                String errorMessage = "Preparation_type cannot be removed as it is used by the following assets: " + assetsWithRemovedPreparationType;
                throw new IllegalArgumentException(errorMessage);
            }
            Specimen updated = new Specimen(existing.institution()
                    , existing.collection()
                    , specimen.barcode()
                    , specimen.specimen_pid()
                    , specimen.preparation_types()
                    , existing.specimen_id()
                    , existing.collection_id()
                    , specimen.role_restrictions());
            specimenRepository
                    .updateSpecimen(updated);

            return updated;
        });
    }

    public List<String> listPreparationTypes() {
        return preparationTypeCache.getPreparationTypes();
    }

    void validateAssetSpecimen(AssetSpecimen assetSpecimen, String defaultInstitution, String defaultCollection) {
        Specimen specimen = resolveAssetSpecimenReference(assetSpecimen, defaultInstitution, defaultCollection);
        if(assetSpecimen.asset_preparation_type != null &&
           (specimen.preparation_types() == null || !specimen.preparation_types().contains(assetSpecimen.asset_preparation_type))) {
            throw new IllegalArgumentException("Specimen has no preparation type that matches asset preparation type: " + assetSpecimen.asset_preparation_type);
        }
    }

    public Specimen resolveAssetSpecimenReference(AssetSpecimen assetSpecimen, String defaultInstitution, String defaultCollection) {
        if (assetSpecimen.specimen_id != null) {
            Optional<Specimen> specimenOpt = findSpecimen(assetSpecimen.specimen_id);
            if (specimenOpt.isEmpty()) {
                throw new IllegalArgumentException("Specimen with id " + assetSpecimen.specimen_id + " doesn't exist");
            }
            Specimen specimen = specimenOpt.get();
            assetSpecimen.specimen = specimen;
            assetSpecimen.specimen_id = specimen.specimen_id();
            assetSpecimen.specimen_pid = specimen.specimen_pid();
            return specimen;
        }

        Specimen embedded = assetSpecimen.specimen;
        String institution = embedded != null && !Strings.isNullOrEmpty(embedded.institution()) ? embedded.institution() : defaultInstitution;
        String collection = embedded != null && !Strings.isNullOrEmpty(embedded.collection()) ? embedded.collection() : defaultCollection;
        String barcode = embedded != null ? embedded.barcode() : null;

        if (Strings.isNullOrEmpty(institution) || Strings.isNullOrEmpty(collection) || Strings.isNullOrEmpty(barcode)) {
            throw new IllegalArgumentException("Each asset specimen must reference an existing specimen by specimen_id or by institution/collection/barcode");
        }

        Optional<Specimen> specimenOpt = findSpecimen(institution, collection, barcode);
        if (specimenOpt.isEmpty()) {
            throw new IllegalArgumentException("Specimen " + institution + "." + collection + "." + barcode + " doesn't exist");
        }
        Specimen specimen = specimenOpt.get();
        assetSpecimen.specimen = specimen;
        assetSpecimen.specimen_id = specimen.specimen_id();
        assetSpecimen.specimen_pid = specimen.specimen_pid();
        return specimen;
    }

    void validateSpecimen(Specimen specimen) {
        if (Strings.isNullOrEmpty(specimen.institution())) {
            throw new IllegalArgumentException("Specimen institution cannot be null");
        }
        if (Strings.isNullOrEmpty(specimen.collection())) {
            throw new IllegalArgumentException("Specimen collection cannot be null");
        }
        if (Strings.isNullOrEmpty(specimen.barcode())) {
            throw new IllegalArgumentException("Specimen barcode cannot be null");
        }
        if (Strings.isNullOrEmpty(specimen.specimen_pid())) {
            specimen.specimen_pid = generateDefaultSpecimenPid(specimen.institution(), specimen.collection(), specimen.barcode());
        }
        if (specimen.preparation_types() == null || specimen.preparation_types().isEmpty()) {
            throw new IllegalArgumentException("a specimen must have at least one preparation_type");
        }
//        if (specimen.asset_preparation_type() != null && !specimen.preparation_types().contains(specimen.asset_preparation_type())) {
//            throw new IllegalArgumentException("Asset preparation_type is not present in preparation types on this specimen");
//        }
        for (String p : specimen.preparation_types()) {
            if (!extendableEnumService.checkExists(ExtendableEnumService.ExtendableEnum.PREPARATION_TYPE, p)) {
                throw new IllegalArgumentException(p + " is not a valid preparation_type");
            }
        }
    }

    private Integer getCollectionId(String collection, String institution) {
        Optional<Collection> collectionInternal = collectionService.findCollectionInternal(collection, institution);
        if (collectionInternal.isEmpty()) {
            throw new IllegalArgumentException("CollectionNotFound not found");
        }
        return collectionInternal.get().collection_id();
    }

    private String generateDefaultSpecimenPid(String institution, String collection, String barcode) {
        if (Strings.isNullOrEmpty(institution) || Strings.isNullOrEmpty(collection) || Strings.isNullOrEmpty(barcode)) {
            throw new IllegalArgumentException("Cannot generate specimen_pid without institution, collection and barcode");
        }
        return institution + "." + collection + "." + barcode;
    }

    Map<String, List<AssetSpecimen>> getMultiAssetSpecimens(Set<String> assetGuids) {
       return jdbi.withHandle(h -> {
           RoleRepository roleRepository = h.attach(RoleRepository.class);
           Set<Integer> specimenIds = new HashSet<>();
           List<AssetSpecimen> assetSpecimens = h.createQuery("""
                            SELECT asset_guid
                                , institution_name
                                , collection_name
                                , barcode
                                , specimen_pid
                                , preparation_types
                                , preparation_type
                                , specimen_id
                                , collection_id
                                , specify_collection_object_attachment_id
                                , asset_detached
                                , asset_specimen_id
                            FROM asset_specimen
                            INNER JOIN specimen USING (specimen_id)
                            LEFT JOIN collection USING (collection_id)
                            WHERE asset_guid in (<assetGuids>) AND asset_detached IS FALSE
                            """)
                .bindList("assetGuids", assetGuids)
                .execute((statement, ctx) -> {
                    try (ctx; var rs = statement.get().getResultSet()) {
                        List<AssetSpecimen> result = new ArrayList<>();
                        while (rs.next()) {
                            String assetGuid = rs.getString("asset_guid");
                            String institutionName = rs.getString("institution_name");
                            String collectionName = rs.getString("collection_name");
                            String barcode = rs.getString("barcode");
                            String specimenPid = rs.getString("specimen_pid");
                            String preparationTypes = rs.getString("preparation_types");
                            String preparationType = rs.getString("preparation_type");
                            int specimenId = rs.getInt("specimen_id");
                            int collectionId = rs.getInt("collection_id");
                            Long specifyCollectionObjectAttachmentId = rs.getLong("specify_collection_object_attachment_id");
                            Long asset_specimenId = rs.getLong("asset_specimen_id");
                            boolean assetDetached = rs.getBoolean("asset_detached");

                            specimenIds.add(specimenId);

                            AssetSpecimen assetSpecimen = new AssetSpecimen(assetDetached, specifyCollectionObjectAttachmentId,preparationType, asset_specimenId, specimenPid, assetGuid,specimenId );
                            assetSpecimen.specimen =
                                    new Specimen(
                                            institutionName,
                                            collectionName,
                                            barcode,
                                            specimenPid,
                                            new HashSet<>(Arrays.asList(preparationTypes.split(","))),
                                            specimenId,
                                            collectionId, new ArrayList<>()

                                    );
                            result.add(assetSpecimen);
//                            assetSpecimensTemp.computeIfAbsent(assetGuid, k -> new ArrayList<>()).add(assetSpecimen);
                        }
                        return result;
                    }
                });
           Map<Integer, List<Role>> roleRestrictionsFromList = roleRepository.getRoleRestrictionsFromList(RestrictedObjectType.SPECIMEN, specimenIds);
           HashMap<String, List<AssetSpecimen>> result = new HashMap<>();
           assetSpecimens.forEach(s -> {
               if(roleRestrictionsFromList.containsKey(s.specimen_id)) {
                   s.specimen.role_restrictions = roleRestrictionsFromList.get(s.specimen_id);
               }
               result.computeIfAbsent(s.asset_guid, k -> new ArrayList<>()).add(s);
           });
           return result;
        });
    }

}

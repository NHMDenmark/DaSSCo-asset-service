package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.repositories.MappingRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MappingService {

    Jdbi jdbi;

    @Inject
    public MappingService(Jdbi jdbi){
        this.jdbi = jdbi;
    }

    public Response addInstitutionsToMapping(Map<String, List<String>> institutionsMapping){

        if (institutionsMapping.keySet().stream().anyMatch(key -> key == null || key.isEmpty())){
            return Response.status(400).entity("One or more Specify Institutions are empty strings.").build();
        }

        if (institutionsMapping.values().stream().flatMap(List::stream).anyMatch(value -> value == null || value.isEmpty())){
            return Response.status(400).entity("One or more ARS Institutions are empty strings.").build();
        }

        return jdbi.inTransaction(handle -> {
            try {
                for (String specifyName : institutionsMapping.keySet()){
                    List<String> arsNames = institutionsMapping.get(specifyName);
                    // Get Institution ID: Create or Get
                    int institutionID = jdbi.onDemand(MappingRepository.class).addInstitutionMapping(specifyName);
                    for (String arsName : arsNames){
                        // Get ARS Collection ID: Create or Get
                        int arsID = jdbi.onDemand(MappingRepository.class).addArsInstitutionMapping(arsName);
                        // Add Mapping:
                        jdbi.onDemand(MappingRepository.class).addMapping(institutionID, arsID);
                    }
                }
                return Response.status(200).entity("Mappings have been saved").build();
            } catch (Exception e){
                handle.rollback();
                return Response.status(500).entity("There has been an error with the mapping. Error: " + e.getMessage()).build();
            }
        });
    }

    public Response getInstitutionMapping(String arsInstitution){
        if (arsInstitution == null || arsInstitution.isEmpty()){
            return Response.status(400).entity("Institution is null or empty").build();
        }
        String found = jdbi.onDemand(MappingRepository.class).getArsInstitutionMapping(arsInstitution);

        if (found == null){
            return Response.status(500).entity("There was no mapping for the ARS Institution").build();
        } else {
            return Response.status(200).entity(found).build();
        }
    }

    public Response updateSpecifyInstitutions(Map<String, String> institutions){

        if (institutions.keySet().stream().anyMatch(key -> key == null || key.isEmpty())){
            return Response.status(400).entity("One or more Keys are null or empty").build();
        }

        if (institutions.values().stream().anyMatch(value -> value == null || value.isEmpty())){
            return Response.status(400).entity("One or more values are null or empty").build();
        }

        jdbi.useTransaction(handle -> {
            for (Map.Entry<String, String> entry : institutions.entrySet()){
                String oldName = entry.getKey();
                String newName = entry.getValue();

                Optional<Integer> institutionId = jdbi.onDemand(MappingRepository.class).findInstitution(oldName);

                institutionId.ifPresent(integer -> jdbi.onDemand(MappingRepository.class).updateSpecifyInstitutionName(newName, integer));
            }
        });

        return Response.status(200).entity("Specify Institutions have been updated").build();
    }

    public Response updateArsInstitution(Map<String, String> institutions){

        if (institutions.keySet().stream().anyMatch(key -> key == null || key.isEmpty())){
            return Response.status(400).entity("One or more Keys are null or empty").build();
        }

        if (institutions.values().stream().anyMatch(value -> value == null || value.isEmpty())){
            return Response.status(400).entity("One or more values are null or empty").build();
        }

        jdbi.useTransaction(handle -> {
            for (Map.Entry<String, String> entry : institutions.entrySet()){
                String oldName = entry.getKey();
                String newName = entry.getValue();

                Optional<Integer> institutionId = jdbi.onDemand(MappingRepository.class).findArsInstitution(oldName);

                institutionId.ifPresent(integer -> jdbi.onDemand(MappingRepository.class).updateArsInstitutionName(newName, integer));
            }
        });

        return Response.status(200).entity("ARS Institutions have been updated").build();
    }

    public Response deleteMappings(Map<String, List<String>> mappings){
        jdbi.useTransaction(handle -> {
            for (Map.Entry<String, List<String>> entry : mappings.entrySet()){
                String specifyInstitution = entry.getKey();
                List<String> arsNames = entry.getValue();
                // Get Specify Institution ID:
                Optional<Integer> specifyId = jdbi.onDemand(MappingRepository.class).findInstitution(specifyInstitution);
                if (specifyId.isPresent()){
                    for (String arsInstitution : arsNames){
                        // Get ARS Institution ID:
                        Optional<Integer> arsId = jdbi.onDemand(MappingRepository.class).findArsInstitution(arsInstitution);
                        if (arsId.isPresent()){
                            jdbi.onDemand(MappingRepository.class).deleteMapping(specifyId.get(), arsId.get());
                            jdbi.onDemand(MappingRepository.class).deleteArsInstitution(arsId.get());
                        }
                    }
                    // check if institutions has any more mappings:
                    int mappingsLeft = jdbi.onDemand(MappingRepository.class).countMappings(specifyId.get());
                    if (mappingsLeft == 0){
                        jdbi.onDemand(MappingRepository.class).deleteSpecifyInstitution(specifyId.get());
                    }
                }
            }

        });
        return Response.status(200).entity("Mappings have been deleted").build();
    }

    public Response addCollectionsToMapping(Map<String, List<String>> collectionsMapping){

        if (collectionsMapping.keySet().stream().anyMatch(key -> key == null || key.isEmpty())){
            return Response.status(400).entity("One or more Specify Collections are empty strings.").build();
        }

        if (collectionsMapping.values().stream().flatMap(List::stream).anyMatch(value -> value == null || value.isEmpty())){
            return Response.status(400).entity("One or more ARS Collections are empty strings.").build();
        }

        return jdbi.inTransaction(handle -> {
            try {
                for (String specifyName : collectionsMapping.keySet()){
                    List<String> arsNames = collectionsMapping.get(specifyName);
                    // Get Collection ID: Create or Get
                    int collectionId = jdbi.onDemand(MappingRepository.class).addCollectionMapping(specifyName);
                    for (String arsName : arsNames){
                        // Get ARS Collection ID: Create or Get
                        int arsID = jdbi.onDemand(MappingRepository.class).addArsCollectionMapping(arsName);
                        // Add Mapping:
                        jdbi.onDemand(MappingRepository.class).addCollectionMapping(collectionId, arsID);
                    }
                }
                return Response.status(200).entity("Mappings have been saved").build();
            } catch (Exception e){
                handle.rollback();
                return Response.status(500).entity("There has been an error with the mapping. Error: " + e.getMessage()).build();
            }
        });
    }
}

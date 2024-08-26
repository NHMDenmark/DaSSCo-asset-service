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
        for (String specifyName : institutionsMapping.keySet()){
            List<String> arsNames = institutionsMapping.get(specifyName);
            // Get Institution ID: Create or Get
            int institutionID = jdbi.onDemand(MappingRepository.class).addInstitutionMapping(specifyName);
            for (String arsName : arsNames){
                // Get ARS Collection ID: Create or Get
                int arsID = jdbi.onDemand(MappingRepository.class).addArsInstitutionMapping(arsName);
                // Add Mapping:
                jdbi.onDemand(MappingRepository.class).addMapping(institutionID, arsID);
                return Response.status(200).entity("Mappings have been saved").build();
            }
        }
        return Response.status(500).entity("There has been an error with the mapping").build();
    }
}

package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.configuration.SpecifyUserConfiguration;
import dk.northtech.dasscoassetservice.domain.SpecifyUser;
import dk.northtech.dasscoassetservice.repositories.SpecifyUserRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SpecifyUserService {
    private static final Logger logger = LoggerFactory.getLogger(SpecifyUserService.class);
    private Jdbi jdbi;

    @Inject
    public SpecifyUserService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<SpecifyUser> getAllUsers() {
        return jdbi.onDemand(SpecifyUserRepository.class).getAllSpecifyUsers();
    }

    public Optional<SpecifyUser> getUserFromInstitution(String institution) {
        return jdbi.onDemand(SpecifyUserRepository.class).getSpecifyUserFromInstitution(institution);
    }

    public Optional<SpecifyUser> getUserFromUsername(String username) {
        return jdbi.onDemand(SpecifyUserRepository.class).getSpecifyUserFromUsername(username);
    }

    public Optional<SpecifyUser> updateUser(String institutionName, SpecifyUser newUser) {
        return jdbi.onDemand(SpecifyUserRepository.class).updateSpecifyUser(institutionName, newUser);
    }

    public Optional<SpecifyUser> deleteUser(String institutionName) {
        return jdbi.onDemand(SpecifyUserRepository.class).deleteSpecifyUser(institutionName);
    }

    public Optional<SpecifyUser> newSpecifyUser(SpecifyUser specifyUser) {
        Optional<SpecifyUser> existingUser = jdbi.onDemand(SpecifyUserRepository.class).getSpecifyUserFromInstitution(specifyUser.institution());
        if (existingUser.isEmpty()) {
            return jdbi.onDemand(SpecifyUserRepository.class).createSpecifyUser(specifyUser);
        } else {
            logger.info("Institution {} already has user with username: {} connected.", specifyUser.institution(), existingUser.get().username());
            return Optional.empty();
        }
    }

}

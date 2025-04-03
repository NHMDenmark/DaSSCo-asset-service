package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.repositories.PipelineRepository;
import dk.northtech.dasscoassetservice.repositories.UserRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private Jdbi jdbi;
    private boolean initialised;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private ConcurrentHashMap<String, User> usernameUserMap = new ConcurrentHashMap<>();
    @Inject
    public UserService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void forceRefreshCache() {
        initUsers(true);
    }
    public void initUsers() {
        initUsers(false);
    }
    public User ensureExists(User user) {
        if(Strings.isNullOrEmpty(user.username)) {
            throw new RuntimeException("User was not found");
        }
        Optional<User> userIfExists = getUserIfExists(user.username);
        if(userIfExists.isPresent()){
            User existing = userIfExists.get();
            if(!Strings.isNullOrEmpty(user.keycloak_id) && !user.keycloak_id.equals(existing.keycloak_id)) {
                existing.keycloak_id = user.keycloak_id;
                updateUser(existing);
                // Return the provided user with the internal id as it may contain keycloak credentials for later use.
                user.dassco_user_id = existing.dassco_user_id;
                return user;
            }
        }
        User persistedUser = persistUser(user);
        user.dassco_user_id = persistedUser.dassco_user_id;
        return user;
    }

    public User persistUser(User user) {
        if(Strings.isNullOrEmpty(user.username)) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if(!this.initialised) {
            initUsers();
        }
        if(this.usernameUserMap.containsKey(user.username)){
            throw new IllegalArgumentException("User already exists");
        }
        jdbi.withHandle(h->{
            UserRepository attach = h.attach(UserRepository.class);
            User persistedUser = attach.insertUser(user);
            user.dassco_user_id = persistedUser.dassco_user_id;
            usernameUserMap.put(persistedUser.username, persistedUser);
            return persistedUser;
        });
        // return orig user as it may have keycloak token for later use.
        return user;
    }

    public void updateUser(User user) {
        jdbi.withHandle(h -> {
            if(user.dassco_user_id == null) {
                throw new IllegalArgumentException("Cannot update user without id");
            }
            UserRepository userRepository = h.attach(UserRepository.class);
            userRepository.UpdateUser(user);
            this.usernameUserMap.put(user.username,user);
            return h;
        });
    }
    public Optional<User> getUserIfExists(String username) {
        if(!this.initialised) {
            initUsers();
        }
        User user = usernameUserMap.get(username);
        if(user == null) {
            return Optional.empty();
        }
        return Optional.of(user);
    }
    private void initUsers(boolean force) {
        synchronized (this) {
            if (!this.initialised || force) {
                jdbi.withHandle(h -> {
                    UserRepository userRepository = h.attach(UserRepository.class);
                    List<User> users = userRepository.listUsers();
                    this.usernameUserMap.clear();
                    for(User user: users) {
                        this.usernameUserMap.put(user.username, user);
                    }
                    logger.info("Loaded {} collections", usernameUserMap.size());
                    return h;
                });
                this.initialised = true;
            }
        }
    }
}

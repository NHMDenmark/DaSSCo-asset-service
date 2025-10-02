package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.repositories.RoleRepository;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoleService {
    private Jdbi jdbi;
    boolean initialised = false;
    private final Set<String> roleCache = new HashSet<>();

    public RoleService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Set<String> getRoles() {
        if(roleCache.isEmpty()) {
            this.initRoles();
        }
        return roleCache;
    }

    public void addRole(String newRole) {
        if(this.getRoles().contains(newRole)) {
            return;
        }
        jdbi.withHandle(h -> {
            RoleRepository roleRepository = h.attach(RoleRepository.class);
            roleRepository.createRole(newRole);
            return h;
        });
        this.roleCache.add(newRole);
    }
    public void initRoles() {
        initRoles(false);
    }
    public void initRoles(boolean force) {
        synchronized (this) {
            if (!this.initialised ||force) {
                jdbi.withHandle(h -> {
                    RoleRepository roleRepository = h.attach(RoleRepository.class);
                    List<String> strings = roleRepository.listRoles();
                    this.roleCache.clear();
                    this.roleCache.addAll(strings);
//                    logger.info("Loaded {} institutions", institutionMap.size());
                    return h;
                });
                this.initialised = true;
            }
        }
    }
}

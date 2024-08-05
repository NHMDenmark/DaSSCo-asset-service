## Running for the first time
* Run ``mvn clean``
  * Refer to README in ./libs for info
* Run ``mvn clean package``

## Running database and keycloak
* We generally run these two from the _dassco-file-proxy_ project
* Refer to that project for information

## Running the app
* Run either by clicking the run button
* Or by using the command ``mvn spring-boot:run``
* Navigate to /angular
  * Run ``npm start``

## Running Age Viewer
* Run ``docker compose -f docker-compose-age-viewer.yaml up --build``
* The compose file connects to the external network of the containers from the dassco-file-proxy project
* It will be on _localhost:3000_ and the values needed are:
  * _connect URL:_ database
  * _port:_ 5432
  * _dbname, user & pass_: dassco_file_proxy
* For seeing the entire schema with all the relations, the query that can be used is:

`SELECT * FROM ag_catalog.cypher('dassco', $$
  MATCH (a)
  OPTIONAL MATCH (a)-[e]->(b)
  RETURN a, e, b
  $$) as (a agtype, e agtype, b agtype);`


## application-local.properties file:
* On the root folder (together with the Docker compose files) an application-local.properties file can be added. 
* If the file is added, it has to be pointed to in the application.properties file by adding `spring.config.import=optional:file:./application-local.properties`
* As the name indicates, the application-local.properties file is where all the configurations exclusive to running the project locally should go, as to not mess with the existing configurations for the project.
* For example, for adding test data to the database (useful locally, problematic when deploying) one can add: `spring.liquibase.contexts=default, development` to the local-application.properties file, and the project will run the liquibase scripts and add the test data.


# DaSSCo Deployment

## Cheatsheet
Once you have signed into the manager node, and created the folder, files and docker-compose.yaml mentioned below. You can start the service like this:
```
docker stack deploy -c /path_to_docker_compose/docker-compose.yaml dassco
```
The last parameter names the stack deployment, in this case "dassco", but you can call it what you want.

Now the services should be running, the shutdown the services again, run this:

```
docker stack rm dassco
```
Again the last parameter is the name of the stack you deployed, this case "dassco".

## Full Docker Swarm Example
Currently setup for localhost
```
version: '3.8'
services:
  asset-service:
    image: nhmdenmark/dassco-asset-service:1.5.1-SNAPSHOT
    deploy:
      mode: replicated
      replicas: 1
#      placement: # Only needed if you want to specify which node it should boot on.
#        constraints:
#          - node.hostname == node2
    ports:
      - target: 8084 # Container Port
        published: 8084
        protocol: tcp
        mode: ingress
    environment:
      CORS_ALLOW_ORIGIN: http://localhost:80,http://localhost:8080
      FILEPROXY_LOCATION: http://file-proxy:8080/file_proxy/api # Routed Internally
      FILE_PROXY_ROOT_URL: http://localhost:8081
      ROOT_URL: http://localhost:8084/ars
      POSTGRES_URL: jdbc:postgresql://postgres:5432/dassco_asset_service
      POSTGRES_USER: dassco_asset_service
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      KEYCLOAK_REALM: dassco
      KEYCLOAK_AUTH_SERVER_URL: http://localhost:8083/keycloak/
      KEYCLOAK_RESOURCE: dassco-asset-service
  file-proxy:
    image: nhmdenmark/dassco-file-proxy:1.4.1-SNAPSHOT
    deploy:
      mode: replicated
      replicas: 1
#      placement: # Only needed if you want to specify which node it should boot on.
#        constraints:
#          - node.hostname == node2
    ports:
      - target: 8080 # Container Port
        published: 8081
        protocol: tcp
        mode: ingress
    environment:
      CORS_ALLOW_ORIGIN: http://localhost:80,http://localhost:8080
      POSTGRES_URL: jdbc:postgresql://postgres:5432/dassco_asset_service
      POSTGRES_USER: dassco_asset_service
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      KEYCLOAK_REALM: dassco
      KEYCLOAK_AUTH_SERVER_URL: http://localhost:8083/keycloak/
      KEYCLOAK_RESOURCE: dassco-asset-service
      KEYCLOAK_ADMIN_REALM: dassco
      KEYCLOAK_ADMIN_CLIENT_ID: dassco-file-proxy
      KEYCLOAK_ADMIN_CLIENT_SECRET: ${CLIENT_SECRET}
      ASSET_SERVICE_ROOT_URL: http://asset-service:8084/ars # Routed Internally
      SFTP_HOST: io.erda.dk
      SFTP_PORT: 22
      SFTP_USERNAME: ${SHARE_USERNAME}
      SFTP_PRIVATE_KEY_LOCATION: /config/private.pem
      SFTP_PRIVATE_KEY_PASSPHRASE: ${PEM_PASSWORD}
      SFTP_REMOTE_FOLDER: /
      SHARE_MOUNT_FOLDER: /workdir
      SHARE_NODE_HOST: http://localhost:8081
    volumes:
      - /root/ars/config:/config:ro         # /root/ars/config contains the private.pem file
      - /root/ars/workdir:/workdir:rw       # /root/ars/workdir contains workInProgress assets
      - /root/ars/file_cache:/file_cache:rw # /root/ars/file_cache contains "cached" files for external publishers or the detailed view
  postgres:
    image: apache/age:release_PG16_1.5.0
    deploy:
      mode: replicated
      replicas: 1
#      placement: # Only needed if you want to specify which node it should boot on.
#        constraints:
#          - node.hostname == node3
    ports:
      - target: 5432
        published: 5432
        protocol: tcp
        mode: ingress
    environment:
      POSTGRES_DB: dassco_asset_service
      POSTGRES_USER: dassco_asset_service
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - /root/ars/postgres_data:/var/lib/postgresql/data:rw # Stores all postgres data in a persistent volume: "/root/ars/postgres_data" (persisted on host machine) 
  keycloak:
    image: quay.io/keycloak/keycloak:21.1.1
    deploy:
      mode: replicated
      replicas: 1
#      placement: # Only needed if you want to specify which node it should boot on.
#        constraints:
#          - node.hostname == node1
    environment:
      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
      KC_HOSTNAME: "localhost:8083"
      KC_DB: "postgres" 
      KC_DB_SCHEMA: "public" 
      KC_DB_URL: "jdbc:postgresql://postgres:5432/dassco_asset_service" 
      KC_DB_USERNAME: "dassco_asset_service" 
      KC_DB_PASSWORD: "${POSTGRES_PASSWORD}"
      KC_HOSTNAME_STRICT_HTTPS: "false"
      KC_HTTP_ENABLED: "true"
      KC_HTTP_RELATIVE_PATH: "/keycloak/"
      KEYCLOAK_ADMIN: DasscoAdmin
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
    ports:
      - target: 8080
        published: 8080
        protocol: tcp
        mode: ingress 
    command:
      - start --proxy edge 
  loadbalancer:
    image: nginx:1.26.0-alpine3.19
    deploy:
      mode: global    # Boots a replica on each node in the swarm.
    ports:
      - target: 80
        published: 80
        protocol: tcp
        mode: host    # Uses a port on the hostmachine
    volumes:
      - /root/ars/config/default.conf:/etc/nginx/conf.d/default.conf:rw   # Contains the routing logic for the loadbalancer
```

## DaSSCo ARS
### Prerequisite
* Requires a running database to boot.
* Requires a File Proxy for all features to work.
* Requires a keycloak to run.

### Available ARS Environment Variables:
#### General Variables
| Environment Variable | Description                                                                                                                                                              | Required |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| CORS_ALLOW_ORIGIN    | Set the CORS_ALLOW_ORIGIN header, it's comma seperated. Example: "http://localhost:80,http://localhost:8080"                                                             | TRUE     |
| ROOT_URL             | Set the root url of the ARS, it's used in the frontend for deep-linking etc. Example: "http://localhost:8084/ars"                                                        | TRUE     |
| FILE_PROXY_ROOT_URL  | Set the root url of the File Proxy, it's used by the backend when creating assets etc. This is usually an internal url. Example: "http://file-proxy:8080/file_proxy/api" | TRUE     |
| FILEPROXY_LOCATION   | Set the root url of the ARS, it's used in the frontend for fecthing thumbnails, openapi.json etc. Example: "http://localhost:8080/file_proxy/api"                        | TRUE     |

#### Keycloak Variables
| Environment Variable     | Description                                                                                             | Required |
|--------------------------|---------------------------------------------------------------------------------------------------------|----------|
| KEYCLOAK_REALM           | Set the name of the keycloak realm, you want to use for authorization. Example: "dassco"                | TRUE     |
| KEYCLOAK_AUTH_SERVER_URL | Set the root url of the keycloak. Example: "http://localhost:8083/" or "http://localhost:8083/keycloak" | TRUE     |
| KEYCLOAK_RESOURCE        | Set the client_id you want to use. Example: "dassco-asset-service"                                      | TRUE     |

#### Database Variables
| Environment Variable        | Description                                                                                                                                                      | Required |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| POSTGRES_URL                | Set the jdbc url for your postgres database, usually an internal url. Example: "jdbc:postgresql://postgres:5432/dassco_asset_service"                            | TRUE     |
| POSTGRES_USER               | Set the username for the database user you want to use. Example: "dassco_asset_service"                                                                          | TRUE     |
| POSTGRES_PASSWORD           | Set the password for the database user you want to use. Example: "PASSWORD"                                                                                      | TRUE     |
| POSTGRES_CONNECTION_TIMEOUT | Set the connectionTimeout. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby | FALSE    |
| POSTGRES_IDLE_TIMEOUT       | Set the idleTimeout. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby       | FALSE    |
| POSTGRES_MAX_LIFETIME       | Set the maxLifetime. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby       | FALSE    |
| POSTGRES_MINIMUM_IDLE       | Set the minimumIdle. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby       | FALSE    |
| POSTGRES_MAXIMUM_POOL_SIZE  | Set the maximumPoolSize. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby   | FALSE    |

## DaSSCo FileProxy
### Prerequisite
* Requires a running database to boot.
* Requires a ARS to run properly.
* Requires a keycloak to run.

### Volumes
Needs 3 folders on the host machine, one containing the private.pem file (/config), one for work in progress assets, and finally one for cached files used when serving files over http(s). Mounted as such:
```
    volumes:
      - /root/ars/config:/config:ro         # /root/ars/config contains the private.pem file
      - /root/ars/workdir:/workdir:rw       # /root/ars/workdir contains workInProgress assets
      - /root/ars/file_cache:/file_cache:rw # /root/ars/file_cache contains "cached" files for external publishers or the detailed view
```
In this case: "/root/ars/config/default.conf" has to exist on the host machine where the loadbalancer is booted.

### Available FileProxy Environment Variables:
#### General Variables
| Environment Variable   | Description                                                                                                                                                      | Required |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| CORS_ALLOW_ORIGIN      | Set the CORS_ALLOW_ORIGIN header, it's comma seperated. Example: "http://localhost:80,http://localhost:8080"                                                     | TRUE     |
| ASSET_SERVICE_ROOT_URL | Set the root url of the File Proxy, it's used by the backend when creating assets etc. This is usually an internal url. Example: "http://asset-service:8084/ars" | TRUE     |
| SHARE_MOUNT_FOLDER     | Set the path to folder where you keep work is progress assets, should match the mount path (- /root/ars/workdir:/workdir:rw). Example: "/workdir"                | TRUE     |
| SHARE_NODE_HOST        | Set the node specific hostname for the deployment, this will be used if there's multiple replicas and no share storage. Example: "http://localhost:8081"         | TRUE     |
| SHARE_DISK_SPACE       | (DEPRECATED) Set the storage amount, that should be used for work in progress assets. Example: "9800" (in mb)                                                    | FALSE    |
| SHARE_CACHE_DISK_SPACE | (DEPRECATED) Set the storage amount, that should be used for caching assets files for serving. Example: "200" (in mb)                                            | FALSE    |
| SHARE_CACHE_FOLDER     | Set the path to folder where you keep cached asset files, should match the mount path (- /root/ars/file_cache:/file_cache:rw). Example: "/file_cache"            | TRUE     |
| SHARE_TOTAL_DISK_SPACE | (DEPRECATED) Set the total storage amount available for the File Proxy. Example: "10000" (in mb)                                                                 | FALSE    |

#### Keycloak Variables
| Environment Variable     | Description                                                                                             | Required |
|--------------------------|---------------------------------------------------------------------------------------------------------|----------|
| KEYCLOAK_REALM           | Set the name of the keycloak realm, you want to use for authorization. Example: "dassco"                | TRUE     |
| KEYCLOAK_AUTH_SERVER_URL | Set the root url of the keycloak. Example: "http://localhost:8083/" or "http://localhost:8083/keycloak" | TRUE     |
| KEYCLOAK_RESOURCE        | Set the client_id you want to use. Example: "dassco-asset-service"                                      | TRUE     |

#### Keycloak Service User
| Environment Variable         | Description                                                                                                                   | Required |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------|----------|
| KEYCLOAK_ADMIN_REALM         | (DEPRECATED, should use existing variable) Set realm you want to use, should be the same as KEYCLOAK_REALM. Example: "dassco" | TRUE     |
| KEYCLOAK_ADMIN_CLIENT_ID     | (DEPRECATED, should rename "_Admin_") Set the client_id for the service-user you want to use. Example: "dassco_file_proxy"    | TRUE     |
| KEYCLOAK_ADMIN_CLIENT_SECRET | (DEPRECATED, should rename "_Admin_") Set the client_secret for the service-user you want to use. Example: "SECRET"           | TRUE     |

#### Database Variables
| Environment Variable        | Description                                                                                                                                                      | Required |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| POSTGRES_URL                | Set the jdbc url for your postgres database, usually an internal url. Example: "jdbc:postgresql://postgres:5432/dassco_file_poxy"                                | TRUE     |
| POSTGRES_USER               | Set the username for the database user you want to use. Example: "dassco_file_proxy"                                                                             | TRUE     |
| POSTGRES_PASSWORD           | Set the password for the database user you want to use. Example: "PASSWORD"                                                                                      | TRUE     |
| POSTGRES_CONNECTION_TIMEOUT | Set the connectionTimeout. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby | FALSE    |
| POSTGRES_IDLE_TIMEOUT       | Set the idleTimeout. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby       | FALSE    |
| POSTGRES_MAX_LIFETIME       | Set the maxLifetime. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby       | FALSE    |
| POSTGRES_MINIMUM_IDLE       | Set the minimumIdle. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby       | FALSE    |
| POSTGRES_MAXIMUM_POOL_SIZE  | Set the maximumPoolSize. Check here for the detailed description: https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby   | FALSE    |

#### ERDA
| Environment Variable         | Description                                                                                                                                   | Required |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|----------|
| ERDA_HTTP                    | Set the root url for ERDA, used for quick access to the assets for serving. Example: "https://sid.erda.dk/share_redirect/USERNAME"            | TRUE     |
| SFTP_HOST                    | Set the SFTP root url for ERDA: Example: "io.erda.dk"                                                                                         | TRUE     |
| SFTP_PORT                    | Set the port for the SFTP for ERDA: Example: "22"                                                                                             | TRUE     |
| SFTP_USERNAME                | Set the username for the user used: Example: "asset_service"                                                                                  | TRUE     |
| SFTP_PRIVATE_KEY_LOCATION    | Set the location for the private key pem file, should match the mounted path ("- /root/ars/config:/config:ro"). Example "/config/private-pem" | TRUE     |
| SFTP_PRIVATE_KEY_PASSPHRASE  | Set the passphrase needed to read the Private key. Example: "PASSWORD"                                                                        | TRUE     |
| SFTP_REMOTE_FOLDER           | Set the path on ERDA where you want to upload the Assets. Example: "/"                                                                        | TRUE     |
| SHARE_MAX_ERDA_SYNC_ATTEMPTS | Set the max attempts, the app tries to sync to ERDA, before setting it as an error (It will retry later). Example: "3"                        | FALSE    |

## DaSSCo Keycloak
### Prerequisite
* Requires a running database to boot.

### Environment Variables
We have boiled it down to baseline configs, but you can change a lot more if you want to, look here to see the options: https://www.keycloak.org/server/all-config

| Environment Variable     | Description                                                                                                                                                                        | Required |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| KC_HEALTH_ENABLED        | Enable health check, can be used for checking if the installation is healthy, check docs here "https://www.keycloak.org/server/all-config#category-metrics". Example: "True"       | FALSE    |
| KC_METRICS_ENABLED       | Enable metrics, can be used for checking resource usage, check docs here "https://www.keycloak.org/server/all-config#category-metrics". Example: "True"                            | FALSE    |
| KC_HOSTNAME              | Set the hostname of you keycloak, check docs here "https://www.keycloak.org/server/all-config#category-hostname_v2". Example: "http://localhost:8083"                              | TRUE     |
| KC_HOSTNAME_STRICT_HTTPS | Allow your server to run http and not complain about a mix of https and http, this option is marked as hidden in the keycloak docs. Example: "true"                                | TRUE     |
| KC_HTTP_ENABLED          | Enable the use of http, check docs here https://www.keycloak.org/server/all-config#category-http. Example "true"                                                                   | TRUE     |
| KC_HTTP_RELATIVE_PATH    | Add a prefix to the keycloak endpoint, check docs here https://www.keycloak.org/server/all-config#category-http. Example "/keycloak/"                                              | TRUE     |
| KEYCLOAK_ADMIN           | Set the admin username for the keycloak admin console: Example: "admin"                                                                                                            | TRUE     |
| KEYCLOAK_ADMIN_PASSWORD  | Set the admin password for the keycloak admin console: Example: "PASSWORD"                                                                                                         | TRUE     |
| KC_DB                    | Set the database type, check docs here "https://www.keycloak.org/server/all-config#category-database". Example: "postgres"                                                         | TRUE     |
| KC_DB_SCHEMA             | Set the schema for database, check docs here "https://www.keycloak.org/server/all-config#category-database". Example: "public"                                                     | TRUE     |
| KC_DB_URL                | Set the jdbc url for the database, check docs here "https://www.keycloak.org/server/all-config#category-database". Example: "jdbc:postgresql://postgres:5432/dassco_asset_service" | TRUE     |
| KC_DB_USERNAME           | Set the username of the user you want to use, check docs here "https://www.keycloak.org/server/all-config#category-database". Example: "keycloak"                                  | TRUE     |
| KC_DB_PASSWORD           | Set the password of the user you want to use, check docs here "https://www.keycloak.org/server/all-config#category-database". Example: "PASSWORD"                                  | TRUE     |

## DaSSCo Apache Age
### Prerequisite
* A persistent volume, unless you don't need the data to be persisted

### Volumes
A folder on the host machine, containing the default.conf file. Mounted as such:
```
    volumes:
      - /root/ars/postgres_data:/var/lib/postgresql/data:rw # Stores all postgres data in a persistent volume: "/root/ars/postgres_data" (persisted on host machine) 
```
In this case: "/root/ars/postgres_data" has to exist on the host machine where the database is booted.

### Environment Variables
| Environment Variable | Description                                                                 | Required |
|----------------------|-----------------------------------------------------------------------------|----------|
| POSTGRES_DB          | Set the name of the database. Example: "dassco_asset_service"               | TRUE     |
| POSTGRES_USER        | Set the username of the database user. Example: "dassco_asset_service_user" | TRUE     |
| POSTGRES_PASSWORD    | Set the passworn of the database used. Example: "PASSWORD"                  | TRUE     |

## DaSSCo Nginx Load Balancer
### Prerequisite
* A configuration file: default.conf. Containing the routing logic. This is required on all nodes, if you run replica mode "global".

### Volumes
A folder on the host machine, containing the default.conf file. Mounted as such:
```
    volumes:
      - /root/ars/config/default.conf:/etc/nginx/conf.d/default.conf:rw   # Contains the routing logic for the loadbalancer
```
In this case: "/root/ars/config/default.conf" has to exist on the host machine where the loadbalancer is booted.

Example default.conf file:
```
server {
    listen 80;
    server_name localhost;	
    client_max_body_size 0; # 0 means no max body size
    location /ars {
        access_log off;
        proxy_pass http://asset_service; # asset_service references an upstream below
        proxy_pass_header Content-Type;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    location /file_proxy {
        access_log off;
        proxy_pass http://file_proxy; # file_proxy references an upstream below
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    location /keycloak {
        access_log off;
        proxy_pass http://idp; # idp references an upstream below
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}

upstream file_proxy {
    server file-proxy:8080;
}

upstream asset_service {
    server asset-service:8084;
}

upstream idp {
    server keycloak:8080;
}
```

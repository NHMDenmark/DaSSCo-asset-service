## Running for the first time
* Run ``mvn clean``
  * Refer to README in ./libs for info
* Run ``mvn clean package``

## Running database and keycloak
* We usually run these from the _dassco-file-proxy_ project.
* BUT ff you prefer to do it from here
  * Make sure keycloak runs on port 8083
  * Run ``docker compose -f docker-compose-keycloak.yaml up --build``
  * Run ``docker compose -f docker-compose-postgres.yaml up --build``
* Run the project either
  * Through Spring Boot (preferred)
    * ``mvn spring-boot:run`` 
  * With docker
    * ``docker compose -f docker-compose-app.yaml up --build``

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
* It will be on _localhost:3000_ and the values needed are:
  * _database:_ database
  * _port:_ 5432
  * _dbname, user & pass_: dassco_file_proxy

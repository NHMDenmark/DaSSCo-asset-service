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
`SELECT * FROM cypher('citation_graph', $$
  MATCH (a)
  OPTIONAL MATCH (a)-[e]->(b)
  RETURN a, e, b
  $$) as (a agtype, e agtype, b agtype);`


## application-local.properties file:
* On the root folder (together with the Docker compose files) an application-local.properties file can be added. 
* If the file is added, it has to be pointed to in the application.properties file by adding `spring.config.import=optional:file:./application-local.properties`
* As the name indicates, the application-local.properties file is where all the configurations exclusive to running the project locally should go, as to not mess with the existing configurations for the project.
* For example, for adding test data to the database (useful locally, problematic when deploying) one can add: `spring.liquibase.contexts=default, development` to the local-application.properties file, and the project will run the liquibase scripts and add the test data.

* ## Documentation
* The documentation from confluence has been partly added to this repository in documentation/confluence-docs and documentation/diagrams. This is mainly the documentation that directly relates to the webapi and the diagrams.
* Furthermore everything else that has to do with project description and agreements have been transferred to dasscos N drive.
* All confluence documents have been converted to pdfs and can be found in N:/SCI-SNM-DigitalCollections/DaSSCo/IT_infrastructure/Storage%20solution/Confluence%20documentation.

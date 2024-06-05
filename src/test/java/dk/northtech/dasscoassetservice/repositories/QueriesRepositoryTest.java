package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Query;
import dk.northtech.dasscoassetservice.domain.QueryField;
import dk.northtech.dasscoassetservice.services.AbstractIntegrationTest;
import dk.northtech.dasscoassetservice.services.QueriesService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Testcontainers
class QueriesRepositoryTest {
    @Inject
    QueriesService queriesService;

    @Test
    public void getNodeProperties() {
//        Map<String, List<String>> nodes = queriesRepository.getNodeProperties();
//        System.out.println(nodes);
    }

    @Test
    public void unwrapQuery() {
        List<QueryField> assWheres = Arrays.asList(
            new QueryField("and", "=", "name", "nt_asset_7"),
            new QueryField("or", "=", "name", "nt_asset_3"),
            new QueryField("and", "=", "status", "WORKING_COPY")
        );
        List<QueryField> instWheres = Arrays.asList(
            new QueryField("and", "=", "name", "test-institution"),
            new QueryField("or", "=", "name", "test-institution-stickers")
        );
        List<QueryField> eventWheres = Arrays.asList(
            new QueryField("and", "=", "name", "UPDATE_ASSET_METADATA"),
            new QueryField("and", "range", "timestamp", "1707221116000#1720267516000")
        );
        List<QueryField> userWheres = Arrays.asList(
            new QueryField("and", "=", "name", "mvb")
        );
        Query query = new Query("Asset", assWheres);
        Query query2 = new Query("Institution", instWheres);
        Query query3 = new Query("Event", eventWheres);
        Query query4 = new Query("User", userWheres);
        List<Query> queries = new ArrayList<>();
        queries.add(query);
        queries.add(query2);
        queries.add(query3);
        queries.add(query4);
        // 1709640316 - 1720181116

//        System.out.println(Instant.now().toEpochMilli());

        this.queriesService.unwrapQuery(queries, 200);
    }

    // todo make test that pushes new asset or smth to the database and then use the query thing to get it out again.



}
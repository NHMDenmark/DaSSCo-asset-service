package dk.northtech.dasscoassetservice.repositories.helpers;

public interface DBConstants {
    public static final String AGE_BOILERPLATE =
            """
            CREATE EXTENSION IF NOT EXISTS age;
            LOAD 'age';
            SET search_path = ag_catalog, "$user", public;
                    """;
}

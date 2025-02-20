package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.repositories.helpers.EnumMapper;
import dk.northtech.dasscoassetservice.services.ExtendableEnumService;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.util.List;

public interface EnumRepository extends SqlObject {

    static String getUpdateSQL(ExtendableEnumService.ExtendableEnum extendableEnum) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (e:<EnumName> {<propertyName>: $name})
                             SET e.<propertyName> = $new_name
                           $$, #params
                        ) as (e agtype);""";
        return sql.replace("<EnumName>", extendableEnum.enumName).replace("<propertyName>", extendableEnum.propertyName);
    }

    static String formatSQL(String sql, ExtendableEnumService.ExtendableEnum enumToUpdate) {
        return sql.replace("<EnumName>", enumToUpdate.enumName).replace("<propertyName>", enumToUpdate.propertyName);
    }

    default void persistEnum(ExtendableEnumService.ExtendableEnum enumToUpdate, String status) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MERGE (e:<EnumName>{<propertyName>: $<propertyName>})
                            RETURN e.<propertyName>
                        $$
                        , #params) as (e agtype);
                        """;

        String sqlFormatted = formatSQL(sql, enumToUpdate);
        try {
            withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeMap name = new AgtypeMapBuilder().add(enumToUpdate.propertyName, status).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                handle.createUpdate(sqlFormatted)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default List<String> listEnum(ExtendableEnumService.ExtendableEnum extendableEnum) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (e:<EnumName>)
                             RETURN e.<propertyName>
                           $$
                        ) as (e agtype);""";

        String formattedSql = formatSQL(sql, extendableEnum);
        try {
            return withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(formattedSql)
                        .map(new EnumMapper()).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default void updateEnum(ExtendableEnumService.ExtendableEnum extendableEnum, String oldName, String newName) {
        String updateSQL = getUpdateSQL(extendableEnum);

        System.out.println(updateSQL);
        try {
            withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                AgtypeMap name = new AgtypeMapBuilder()
                        .add("name", oldName)
                        .add("new_name", newName)
                        .build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.createUpdate(updateSQL)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    default void deleteEnum(ExtendableEnumService.ExtendableEnum enumToDelete, String valueToDelete) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (e:<EnumName>{<property_name>: $<property_name>)
                             DELETE e
                           $$
                        ) as (e agtype);""";

        String formattedSQL = formatSQL(sql, enumToDelete);
        try {
            withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                AgtypeMap name = new AgtypeMapBuilder().add( enumToDelete.propertyName,valueToDelete).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

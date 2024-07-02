package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Role;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


public interface RoleRepository extends SqlObject {
    //    private Jdbi jdbi;
//    private DataSource dataSource;
    default void boilerplate() {
        withHandle(handle -> {
            Connection connection = handle.getConnection();
            try {
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
            "LOAD 'age';\n" +
            "SET search_path = ag_catalog, \"$user\", public;";
//    @Inject
//    public RoleRepository(Jdbi jdbi, DataSource dataSource) {
//        this.dataSource = dataSource;
//        this.jdbi = jdbi;
//    }

    @Transaction
    default void removeAllRestrictions(RestrictedObjectType object, String identifier) {
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                    , $$
                         MATCH (""")
                .append("o:")
                .append(object.objectName)
                .append("{").append(object.identifierName).append(": $iden})-[rt:RESTRICTED_TO]->(r:Role)\n")
                .append("DELETE rt\n")
                .append("""
                         $$
                            , #params) as (a agtype);
                        """);
        String query = sb.toString();
        AgtypeMapBuilder builder = new AgtypeMapBuilder();
        AgtypeMap agtypeMap = builder.add("iden", identifier)
                .build();
        Agtype agtype = AgtypeFactory.create(agtypeMap);
        withHandle(h -> {
            h.createUpdate(query)
                    .bind("params", agtype)
                    .execute();
            return h;
        });

    }

    @Transaction
    default void setRoleRestriction(RestrictedObjectType object, String identifier, List<Role> roles) {
        //remove all existing restrictions and overwrite with new ones if provided
        boilerplate();
        removeAllRestrictions(object, identifier);
        if (roles.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                    , $$
                         MATCH (""")
                .append("o:")
                .append(object.objectName)
                .append("{").append(object.identifierName).append(": $identifier})\n");
        for (int i = 0; i < roles.size(); i++) {
            sb.append("     MERGE(r").append(i).append(":Role{name: $r").append(i).append("_name})\n");
        }
        AgtypeMapBuilder builder = new AgtypeMapBuilder();
        builder.add("identifier", identifier);
        for (int i = 0; i < roles.size(); i++) {
            sb.append("     MERGE(o)-[rt").append(i).append(":RESTRICTED_TO]-").append("(r").append(i).append(")\n");
            builder.add("r" + i + "_name", roles.get(i).name());
        }
        sb.append("""
                 $$
                    , #params) as (a agtype);
                """);
        Agtype agtype = AgtypeFactory.create(builder.build());
        String query = sb.toString();
        withHandle(h -> {
            h.createUpdate(query)
                    .bind("params", agtype)
                    .execute();
            return h;
        });
    }

    @Transaction
    default void setRoleRestrictionCollection(Collection collection, Institution institution, List<Role> roles) {
        //remove all existing restrictions and overwrite with new ones if provided
        boilerplate();
        StringBuilder sb1 = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                    , $$
                        MATCH (c:Collection{name: $collection_name})-[USED_BY]-(i:Institution{name: $institution_name}) 
                        MATCH (c)-[rt:RESTRICTED_TO]->(r:Role)\n
                        DELETE rt
                 $$
                    , #params) as (a agtype);
                """);
        String query = sb1.toString();
        System.out.println(query);
        AgtypeMapBuilder builder = new AgtypeMapBuilder();
        AgtypeMap agtypeMap = builder
                .add("collection_name", collection.name())
                .add("institution_name", institution.name())
                .build();
        Agtype agtype = AgtypeFactory.create(agtypeMap);
        withHandle(h -> {
            h.createUpdate(query)
                    .bind("params", agtype)
                    .execute();
            return h;
        });

        if (roles.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                    , $$
                         MATCH (c:Collection{name: $collection_name})-[USED_BY]-(i:Institution{name: $institution_name})""");
        for (int i = 0; i < roles.size(); i++) {
            sb.append("     MERGE(r").append(i).append(":Role{name: $r").append(i).append("_name})\n");
        }
        AgtypeMapBuilder builder2 = new AgtypeMapBuilder();
        builder2
                .add("collection_name", collection.name())
                .add("institution_name", institution.name());
        for (int i = 0; i < roles.size(); i++) {
            sb.append("     MERGE(c)-[rt").append(i).append(":RESTRICTED_TO]-").append("(r").append(i).append(")\n");
            builder2.add("r" + i + "_name", roles.get(i).name());
        }
        sb.append("""
                 $$
                    , #params) as (a agtype);
                """);
        Agtype agtype1 = AgtypeFactory.create(builder2.build());
        String query2 = sb.toString();
        System.out.printf(query2);
        withHandle(h -> {
            h.createUpdate(query2)
                    .bind("params", agtype1)
                    .execute();
            return h;
        });
    }
}

package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


public interface RoleRepository extends SqlObject {

    default String getDeleteSql(RestrictedObjectType object) {
        return "DELETE FROM " + object.objectName + "_role_restriction WHERE " + object.identifierName + " =:param" ;
    }

    default String getFindSql(RestrictedObjectType object) {
        return "SELECT role FROM " + object.objectName + "_role_restriction WHERE " + object.identifierName + " =:param" ;
    }

    @SqlUpdate("INSERT INTO role(role) VALUES (:role)")
    void createRole(String role);

    @SqlQuery("SELECT r.role FROM role r")
    List<String> listRoles();

    @Transaction
    default void removeAllRestrictions(RestrictedObjectType restrictedObject, String identifier) {
        String deleteSql = getDeleteSql(restrictedObject);
        withHandle(h -> {
            h.createUpdate(deleteSql)
                    .bind("param", identifier)
                    .execute();
            return h;
        });
    }

    @Transaction
    default void removeAllRestrictions(RestrictedObjectType object, int identifier) {
        String deleteSql = getDeleteSql(object);
        withHandle(h -> {
            h.createUpdate(deleteSql)
                    .bind("param", identifier)
                    .execute();
            return h;
        });
    }

    default String getInsertSql(RestrictedObjectType object) {
        return "INSERT INTO " + object.objectName + "_role_restriction(" + object.identifierName + ", role) VALUES (:param1, :param2)" ;
    }

    @Transaction
    default void setRestrictions(RestrictedObjectType object, List<Role> roles, String identifier) {
        withHandle(h -> {
            //Clear restriction
            removeAllRestrictions(object, identifier);
            String insertSql = getInsertSql(object);
            for (Role r : roles) {
                h.createUpdate(insertSql)
                        .bind("param1", identifier)
                        .bind("param2", r.name()).execute();
            }
            return h;
        });
    }



    @Transaction
    default void setRestrictions(RestrictedObjectType object, List<Role> roles, int identifier) {
        withHandle(h -> {
            //Clear restriction
            removeAllRestrictions(object, identifier);
            String insertSql = getInsertSql(object);
            for (Role r : roles) {
                h.createUpdate(insertSql)
                        .bind("param1", identifier)
                        .bind("param2", r.name()).execute();
            }
            return h;
        });
    }

    default String getSelect(RestrictedObjectType object) {
        return "SELECT * FROM " + object.objectName + "_role_restriction" ;
    }


    default List<Role> findRoleRestrictions(RestrictedObjectType object, int identifier) {
        return withHandle(h -> {
            return h.createQuery(getFindSql(object)).bind("param", identifier)
                    .mapTo(Role.class).list();
        });
    }

    @SqlQuery("SELECT * FROM collection_role_restriction")
    List<CollectionRoleRestriction> getCollectionRoleRestrictions();

    @SqlQuery("SELECT * FROM institution_role_restriction")
    List<InstitutionRoleRestriction> getInstitutionRoleRestriction();



}

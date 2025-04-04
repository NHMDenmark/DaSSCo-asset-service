package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface UserRepository extends SqlObject {


    @SqlQuery("SELECT * FROM dassco_user WHERE dassco_user.username = :username")
    User getUserByUsername(String username);

    @SqlQuery("SELECT * FROM dassco_user")
    List<User> listUsers();
    @SqlQuery("SELECT * FROM dassco_user WHERE user.keycloak_id = :keycloak_id")
    User getUserByKeycloakId(String username);

    @SqlQuery("""
    SELECT username 
    FROM digitiser_list 
    LEFT JOIN dassco_user USING (dassco_user_id)
    WHERE asset_guid = :assetId
    """)
    List<String> getDigitiserList(String assetId);

    @GetGeneratedKeys
    @SqlUpdate("""
    INSERT INTO dassco_user(username, keycloak_id) 
    VALUES (:username, :keycloak_id)
    RETURNING *;
    """)
    User insertUser(@BindFields User user);

    @SqlUpdate("UPDATE dassco_user SET keycloak_id = :keycloak_id WHERE username = :username")
    void UpdateUser(@BindFields User user);

    @Transaction
    default boolean isUserOwnerOfAssetGroup(String groupName, String username){
        throw new UnsupportedOperationException("Not implemented");
    }

    @SqlUpdate("INSERT INTO digitiser_list(asset_guid, dassco_user_id) VALUES (:assetGuid, :dasscoUserId)")
    void addDigitiser(String assetGuid, int dasscoUserId);

    @Transaction
    default boolean userHasAccessToAsset(String user, String assetGuid){
        throw new UnsupportedOperationException("Not implemented");
    }

}

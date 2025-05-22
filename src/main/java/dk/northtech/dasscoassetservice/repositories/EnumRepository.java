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
        String sql = "UPDATE <EnumName> SET <EnumName> = :new_name WHERE <EnumName> = :name ";
        return sql.replace("<EnumName>", extendableEnum.enumName);
    }

    static String formatSQL(String sql, ExtendableEnumService.ExtendableEnum enumToUpdate) {
        return sql.replace("<EnumName>", enumToUpdate.enumName);
    }

    default void persistEnum(ExtendableEnumService.ExtendableEnum enumToUpdate, String status) {
        String sql = "INSERT INTO <EnumName>(<EnumName>) VALUES(:name)";
        String sqlFormatted = formatSQL(sql, enumToUpdate);
        try {
            withHandle(handle -> {

                handle.createUpdate(sqlFormatted)
                        .bind("name", status)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default List<String> listEnum(ExtendableEnumService.ExtendableEnum extendableEnum) {
        String sql = "SELECT * FROM <EnumName>";

        String formattedSql = formatSQL(sql, extendableEnum);
        try {
            return withHandle(handle -> {
                // We have to register the type

                return handle.createQuery(formattedSql)
                        .mapTo(String.class).list();
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

                handle.createUpdate(updateSQL)
                        .bind("new_name", newName)
                        .bind("name", oldName)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

}

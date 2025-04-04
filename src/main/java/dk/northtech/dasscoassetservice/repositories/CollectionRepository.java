package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.helpers.CollectionMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends SqlObject {
//    private Jdbi jdbi;
//    private DataSource dataSource;

//    @Inject
//    public CollectionRepository(Jdbi jdbi, DataSource dataSource) {
//        this.dataSource = dataSource;
//        this.jdbi = jdbi;
//    }


    @Transaction
    @SqlUpdate("""
    INSERT INTO collection(collection_name, institution_name) 
    VALUES (:name, :institution) 
    RETURNING *
    """)
    @GetGeneratedKeys("collection_id")
    Collection persistCollection(@BindMethods Collection collection);



    @SqlQuery("SELECT * FROM collection")
    List<Collection> readAll();
}

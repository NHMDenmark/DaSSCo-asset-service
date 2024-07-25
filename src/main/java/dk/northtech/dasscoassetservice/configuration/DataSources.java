package dk.northtech.dasscoassetservice.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dk.northtech.dasscoassetservice.domain.Directory;
import jakarta.inject.Named;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;


import javax.sql.DataSource;

@Configuration
public class DataSources {

  @Bean(name="admin")
  @Primary
  public DataSource adminDataSource(@Named("admin-config")HikariConfig hikariConfig) {
    return new HikariDataSource(hikariConfig);
  }

  // Using an explicit bean to carry the configuration allows the tooling to recognize the Hikari-specific property
  // names and, say, offer them as autocompletion in the property file.
  @Bean(name="admin-config")
  @ConfigurationProperties("datasource.admin")
  public HikariConfig adminHikariConfig() {
    return new HikariConfig();
  }

  @Bean(name="readonly")
  @DependsOn("liquibase")
  public DataSource readonlyDataSource(@Named("readonly-config") HikariConfig hikariConfig){
    return new HikariDataSource(hikariConfig);
  }

  @Bean(name="readonly-config")
  @ConfigurationProperties("datasource.readonly")
  public HikariConfig readonlyHikariConfig(){
    return new HikariConfig();
  }

  @Bean
  public Jdbi jdbi(DataSource dataSource) {
    return Jdbi.create(dataSource)
            .installPlugin(new PostgresPlugin())
            .installPlugin(new SqlObjectPlugin())
            .installPlugin(new Jackson2Plugin())
            .registerRowMapper(ConstructorMapper.factory(Directory.class))
            .setSqlParser(new HashPrefixSqlParser());
  }

}


package dk.northtech.dasscoassetservice.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dk.northtech.dasscoassetservice.domain.Directory;
import jakarta.inject.Named;
import liquibase.integration.spring.SpringLiquibase;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;


import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSources {

  @Value("${datasource.readonly.username:}")
  private String readonlyUsername;

  @Value("${datasource.readonly.password:}")
  private String readonlyPassword;

  @Bean(name="admin")
  public DataSource adminDataSource(@Named("admin-config")HikariConfig hikariConfig) {
    return new HikariDataSource(hikariConfig);
  }

  @Bean(name="readonly")
  @DependsOn("liquibase")
  public DataSource readonlyDataSource(@Named("readonly-config") HikariConfig hikariConfig){
    return new HikariDataSource(hikariConfig);
  }

  @Bean(name = "test")
  public DataSource dataSource(@Named("admin-config") HikariConfig hikariConfig) {
    return new HikariDataSource(hikariConfig);
  }

  // Using an explicit bean to carry the configuration allows the tooling to recognize the Hikari-specific property
  // names and, say, offer them as autocompletion in the property file.
  @Bean(name="admin-config")
  @ConfigurationProperties("datasource.admin")
  public HikariConfig adminHikariConfig() {
    return new HikariConfig();
  }

  @Bean(name="readonly-config")
  @ConfigurationProperties("datasource.readonly")
  public HikariConfig readonlyHikariConfig(){
    return new HikariConfig();
  }

  /*
  @Bean(name = "test-config")
  public HikariConfig testHikariConfig(){
    return new HikariConfig();
  }*/

  @Bean
  public Jdbi jdbi(@Named("admin") DataSource dataSource) {
    return Jdbi.create(dataSource)
            .installPlugin(new PostgresPlugin())
            .installPlugin(new SqlObjectPlugin())
            .installPlugin(new Jackson2Plugin())
            .registerRowMapper(ConstructorMapper.factory(Directory.class))
            .setSqlParser(new HashPrefixSqlParser());
  }

  @Bean
  public SpringLiquibase liquibase(@Named("admin") DataSource dataSource) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("classpath:/liquibase/changelog-master.xml");
    Map<String, String> parameters = new HashMap<>();
    parameters.put("readonly.username", readonlyUsername);
    parameters.put("readonly.password", readonlyPassword);
    liquibase.setChangeLogParameters(parameters);
    return liquibase;
  }

  @Bean
  public SpringLiquibase testLiquibase(@Named("test") DataSource dataSource) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("classpath:/liquibase/changelog-master.xml");
    Map<String, String> parameters = new HashMap<>();
    parameters.put("readonly.username", readonlyUsername);
    parameters.put("readonly.password", readonlyPassword);
    liquibase.setChangeLogParameters(parameters);
    return liquibase;
  }

}


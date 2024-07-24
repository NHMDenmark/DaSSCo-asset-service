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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSources {

  @Bean
  public DataSource dataSource(HikariConfig hikariConfig) {
    return new HikariDataSource(hikariConfig);
  }

  // Using an explicit bean to carry the configuration allows the tooling to recognize the Hikari-specific property
  // names and, say, offer them as autocompletion in the property file.
  @Bean
  @ConfigurationProperties("datasource")
  public HikariConfig hikariConfig() {
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


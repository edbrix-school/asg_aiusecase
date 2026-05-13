package com.asg.aiusecase.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class CommonDbConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties vectorDbDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = {"dataSource", "vectorDbDataSource"})
    @Primary
    @FlywayDataSource
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource vectorDbDataSource(
            @Qualifier("vectorDbDataSourceProperties") DataSourceProperties vectorDbDataSourceProperties) {
        return vectorDbDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("vectorDbDataSource") DataSource vectorDbDataSource) {
        return new JdbcTemplate(vectorDbDataSource);
    }

    @Bean
    @ConfigurationProperties("app.common-db.datasource")
    public DataSourceProperties commonDbDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.common-db.datasource.hikari")
    public DataSource commonDbDataSource(
            @Qualifier("commonDbDataSourceProperties") DataSourceProperties commonDbDataSourceProperties) {
        return commonDbDataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public JdbcTemplate commonDbJdbcTemplate(@Qualifier("commonDbDataSource") DataSource commonDbDataSource) {
        return new JdbcTemplate(commonDbDataSource);
    }
}

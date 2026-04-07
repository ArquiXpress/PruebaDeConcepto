package com.arquixpress.marketplace.catalog;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class ReadReplicaConfig {
    @Bean
    @ConditionalOnProperty(name = "app.read-replica.enabled", havingValue = "true")
    public NamedParameterJdbcTemplate catalogReadReplicaJdbcTemplate(
            @Value("${app.read-replica.datasource.url}") String jdbcUrl,
            @Value("${app.read-replica.datasource.username}") String username,
            @Value("${app.read-replica.datasource.password}") String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(4);
        dataSource.setPoolName("catalog-read-replica");
        return new NamedParameterJdbcTemplate(dataSource);
    }
}

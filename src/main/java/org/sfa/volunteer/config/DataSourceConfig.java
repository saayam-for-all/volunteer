package org.sfa.volunteer.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${db.us.url}")
    private String usDbUrl;

    @Value("${db.in.url}")
    private String inDbUrl;

    @Value("${db.username.eu}")
    private String dbUsernameEu;

    @Value("${db.password.eu}")
    private String dbPasswordEu;

    @Value("${db.username.us}")
    private String dbUsernameUS;

    @Value("${db.password.us}")
    private String dbPasswordUS;

    @Value("${db.eu.schema}")
    private String euSchema;

    @Value("${db.us.schema}")
    private String usSchema;

    @Bean
    public DataSource dataSource() {
        DynamicRoutingDataSource dynamicDataSource = new DynamicRoutingDataSource();

        HikariDataSource usDb = createDataSource(usDbUrl, dbUsernameUS, dbPasswordUS, usSchema);
        HikariDataSource inDb = createDataSource(inDbUrl, dbUsernameEu, dbPasswordEu, euSchema);

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("NonEU", usDb);
        targetDataSources.put("EU", inDb);

        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(usDb); // Fallback DB
        return dynamicDataSource;
    }

    private HikariDataSource createDataSource(String url, String username, String password, String schema) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.addDataSourceProperty("currentSchema", schema); // Sets the default schema
        return dataSource;
    }
}

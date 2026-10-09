package com.mycompany.myapp.config;

import com.mycompany.myapp.service.config.LiveDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LiveDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        return new LiveDataSource(properties);
    }
}

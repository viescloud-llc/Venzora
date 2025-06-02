package com.viescloud.llc.venzora.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseConfiguration {
    
    // Type
    @Value("${venzora.database.type:h2}")
    private String databaseType;
    
    // H2 Configuration
    @Value("${VENZORA_H2_DB:/etc/venzora/db/h2}")
    private String h2DbPath;
    
    @Value("${VENZORA_H2_USERNAME:sa}")
    private String h2Username;
    
    @Value("${VENZORA_H2_PASSWORD:password}")
    private String h2Password;
    
    // MySQL Configuration
    @Value("${VENZORA_MYSQL_HOST:localhost}")
    private String mysqlHost;
    
    @Value("${VENZORA_MYSQL_PORT:3306}")
    private String mysqlPort;
    
    @Value("${VENZORA_MYSQL_DB:venzora}")
    private String mysqlDb;
    
    @Value("${VENZORA_MYSQL_USERNAME:venzora}")
    private String mysqlUsername;
    
    @Value("${VENZORA_MYSQL_PASSWORD:password}")
    private String mysqlPassword;
    
    @Value("${VENZORA_MYSQL_USE_SSL:false}")
    private boolean mysqlUseSSL;
    
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        
        if ("mysql".equalsIgnoreCase(databaseType)) {
            log.info("Configuring MySQL database");
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=%s&allowPublicKeyRetrieval=true", 
                mysqlHost, mysqlPort, mysqlDb, mysqlUseSSL);
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(mysqlUsername);
            dataSource.setPassword(mysqlPassword);
        } 
        else if("h2".equalsIgnoreCase(databaseType)) {
            // Default to H2
            log.info("Configuring H2 database");
            dataSource.setDriverClassName("org.h2.Driver");
            String jdbcUrl = String.format("jdbc:h2:file:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE;" + 
                "CACHE_SIZE=8192;MAX_MEMORY_ROWS=10000;MV_STORE=TRUE;PAGE_SIZE=1024;COMPRESS=TRUE", h2DbPath);
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(h2Username);
            dataSource.setPassword(h2Password);
        }
        else {
            log.info("Configuring H2 in-memory database");
            dataSource.setDriverClassName("org.h2.Driver");
            String jdbcUrl = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;" + 
                "CACHE_SIZE=8192;MAX_MEMORY_ROWS=10000;MV_STORE=FALSE", "h2DbName");
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(h2Username);
            dataSource.setPassword(h2Password);
        }
        
        // Common HikariCP settings
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(2);
        dataSource.setIdleTimeout(30000);
        
        return dataSource;
    }
}
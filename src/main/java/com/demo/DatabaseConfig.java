package com.demo;

import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {
    private String dbUrl;

    private String dbUsername;

    private String dbPassword;

    // Getters for the properties
    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
}
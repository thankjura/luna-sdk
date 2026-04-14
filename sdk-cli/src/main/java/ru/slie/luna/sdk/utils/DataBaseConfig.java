package ru.slie.luna.sdk.utils;

import org.postgresql.Driver;

import java.util.Properties;

public class DataBaseConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String database;

    public DataBaseConfig() {
        this.host = "localhost";
        this.port = 5432;
        this.username = "luna";
        this.password = "luna";
        this.database = "luna";
    }

    public DataBaseConfig(String dbUri) {
        this();
        Properties props = Driver.parseURL(dbUri, null);
        if (props == null) {
            throw new IllegalArgumentException("Failed parse db uri");
        }
        this.host = props.getProperty("host");
        this.port = Integer.parseInt(props.getProperty("port"));
        this.username = props.getProperty("username");
        this.password = props.getProperty("password");
        this.database = props.getProperty("database");
    }

    public String getDbUri() {
        return String.format("postgresql://%s:%s@%s:%s/%s", username, password, host, port, database);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String toYamlConfig() {
        return "host: " + host + "\n" +
               "port: " + port + "\n" +
               "username: " + username + "\n" +
               "password: " + password + "\n" +
               "database: " + database + "\n";
    }
}

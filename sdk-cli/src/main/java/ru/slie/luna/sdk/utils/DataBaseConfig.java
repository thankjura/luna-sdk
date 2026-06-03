package ru.slie.luna.sdk.utils;

import org.postgresql.Driver;
import org.postgresql.PGProperty;

import java.util.Properties;

public class DataBaseConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String database;
    private static final I18nResolver i18n = new I18nResolver();

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
            throw new IllegalArgumentException(i18n.getString("database.provider.parse_uri_error"));
        }
        try {
            this.host = PGProperty.PG_HOST.getOrDefault(props);
            this.port = PGProperty.PG_PORT.getInt(props);
            this.username = PGProperty.USER.getOrDefault(props);
            this.password = PGProperty.PASSWORD.getOrNull(props);
            this.database = PGProperty.PG_DBNAME.getOrDefault(props);
        } catch (Exception e) {
            throw new IllegalArgumentException(i18n.getString("database.provider.parse_uri_error"));
        }
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

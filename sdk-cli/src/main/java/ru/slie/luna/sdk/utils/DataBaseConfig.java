package ru.slie.luna.sdk.utils;

import org.postgresql.Driver;
import org.postgresql.PGProperty;

import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.LinkedHashMap;
import java.util.Map;
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

    /**
     * JDBC uri without password, safe for logging.
     */
    public String getDisplayUri() {
        return String.format("jdbc:postgresql://%s:%s/%s?user=%s", host, port, database, username);
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

    /**
     * database.yml with the connection of this config. Other settings of an existing file (maximumPoolSize,
     * timeouts, showSql) are kept, so they can be tuned in luna-home between runs.
     *
     * @param existing current content of database.yml, null if there is none
     */
    @SuppressWarnings("unchecked")
    public String toYamlConfig(String existing) {
        YAMLMapper mapper = new YAMLMapper();
        Map<String, Object> values = new LinkedHashMap<>();
        if (existing != null && !existing.isBlank()) {
            try {
                Object parsed = mapper.readValue(existing, Map.class);
                if (parsed instanceof Map<?, ?> map) {
                    values.putAll((Map<String, Object>) map);
                }
            } catch (RuntimeException ignored) {
                // broken file: rewrite it with the connection only
            }
        }
        values.put("host", host);
        values.put("port", port);
        values.put("username", username);
        values.put("password", password == null ? "" : password);
        values.put("database", database);
        return mapper.writeValueAsString(values);
    }
}

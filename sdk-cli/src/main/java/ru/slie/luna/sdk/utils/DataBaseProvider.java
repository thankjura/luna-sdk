package ru.slie.luna.sdk.utils;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataBaseProvider implements AutoCloseable {
    private final PostgreSQLContainer<?> postgreSQLContainer;
    private static final I18nResolver i18n = new I18nResolver();
    private final DataBaseConfig dataBaseConfig;

    public DataBaseProvider(Path targetDir, DataBaseConfig config) throws IOException, ParseValueException {
        if (config != null) {
            dataBaseConfig = config;
            postgreSQLContainer = null;
        } else {
            dataBaseConfig = new DataBaseConfig();
            Path mongoDataDir = targetDir.resolve("database");
            Files.createDirectories(mongoDataDir);
            org.apache.logging.log4j.Logger dataBaseLogger = LoggingUtils.getFileLogger("db", targetDir.resolve("database.log").toAbsolutePath().toString(), false);
            postgreSQLContainer = new PostgreSQLContainer<>("postgres:18");
            postgreSQLContainer.withReuse(false).withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(dataBaseLogger.getName())));
            postgreSQLContainer.start();
            dataBaseConfig.setHost(postgreSQLContainer.getHost());
            dataBaseConfig.setPort(postgreSQLContainer.getMappedPort(5432));
            dataBaseConfig.setUsername(postgreSQLContainer.getUsername());
            dataBaseConfig.setPassword(postgreSQLContainer.getPassword());
            dataBaseConfig.setDatabase(postgreSQLContainer.getDatabaseName());
        }
    }

    public DataBaseConfig getConfig() {
        return dataBaseConfig;
    }

    @Override
    public void close() {
        if (postgreSQLContainer != null) {
            postgreSQLContainer.stop();
        }
    }

    public boolean isContainer() {
        return postgreSQLContainer != null;
    }
}

package ru.slie.luna.sdk.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Provides database for the test instance: either an external one (--db-uri) or a PostgreSQL docker container.
 * <p>
 * Container data is stored in {@code <state dir>/database} next to luna-home, so the database and luna-home
 * (index, attachments, installed plugins) are always kept, moved and deleted together
 * ({@code mvn clean} for a plugin, {@code --reset}, or removing the --workdir directory).
 */
public class DataBaseProvider implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger("luna-sdk");
    private static final I18nResolver i18n = new I18nResolver();
    private static final String POSTGRES_MAJOR = "18";
    private static final String POSTGRES_IMAGE = "postgres:" + POSTGRES_MAJOR;
    private static final String POSTGRES_DATA_PATH = "/var/lib/postgresql";
    private static final String READY_LOG_REGEX = ".*database system is ready to accept connections.*\\s";

    private final PostgreSQLContainer<?> postgreSQLContainer;
    private final DataBaseConfig dataBaseConfig;

    public DataBaseProvider(Path stateDir, Path databaseDir, DataBaseConfig config) throws IOException {
        if (config != null) {
            dataBaseConfig = config;
            postgreSQLContainer = null;
            return;
        }

        Files.createDirectories(databaseDir);
        // PGDATA of the postgres:18 image is /var/lib/postgresql/18/docker
        boolean hasData = Files.isRegularFile(databaseDir.resolve(POSTGRES_MAJOR).resolve("docker").resolve("PG_VERSION"));
        log.info(i18n.getString(hasData ? "database.provider.data_reused" : "database.provider.data_created", databaseDir.toString()));

        org.apache.logging.log4j.Logger dataBaseLogger = LoggingUtils.getFileLogger("db", stateDir.resolve("database.log").toString(), false);
        postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE);
        postgreSQLContainer.withReuse(false)
                           .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(dataBaseLogger.getName())))
                           .withFileSystemBind(databaseDir.toString(), POSTGRES_DATA_PATH, BindMode.READ_WRITE)
                           // On the first start the entrypoint restarts postgres after init, so the message is printed twice
                           .waitingFor(Wait.forLogMessage(READY_LOG_REGEX, hasData ? 1 : 2).withStartupTimeout(Duration.ofMinutes(2)));

        // Run postgres as the directory owner: otherwise the files become owned by the container's postgres
        // user and the current user can't delete them (mvn clean, --reset)
        getOwner(databaseDir).ifPresent(owner -> postgreSQLContainer.withCreateContainerCmdModifier(cmd -> cmd.withUser(owner)));
        postgreSQLContainer.start();

        dataBaseConfig = new DataBaseConfig();
        dataBaseConfig.setHost(postgreSQLContainer.getHost());
        dataBaseConfig.setPort(postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        dataBaseConfig.setUsername(postgreSQLContainer.getUsername());
        dataBaseConfig.setPassword(postgreSQLContainer.getPassword());
        dataBaseConfig.setDatabase(postgreSQLContainer.getDatabaseName());
    }

    /**
     * @return "uid:gid" of the directory on unix systems, empty elsewhere (Docker Desktop on Windows maps owners itself)
     */
    private static Optional<String> getOwner(Path dir) {
        try {
            Object uid = Files.getAttribute(dir, "unix:uid");
            Object gid = Files.getAttribute(dir, "unix:gid");
            // postgres refuses to run as root, let the image entrypoint handle that case
            return Integer.valueOf(0).equals(uid) ? Optional.empty() : Optional.of(uid + ":" + gid);
        } catch (UnsupportedOperationException | IllegalArgumentException | IOException e) {
            return Optional.empty();
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

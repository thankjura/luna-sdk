package ru.slie.luna.sdk.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Provides database for the test instance: either an external one (--db-uri) or a PostgreSQL docker container.
 * <p>
 * Container data is kept in a named docker volume bound to the project. The volume lives exactly as long as
 * luna-home does: a marker file in luna-home pairs them, and when it is missing (first run, {@code mvn clean},
 * {@code --reset}) both are recreated by the run command, so the database never gets out of sync with the index,
 * attachments and installed plugins in luna-home.
 */
public class DataBaseProvider implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger("luna-sdk");
    private static final I18nResolver i18n = new I18nResolver();
    private static final String POSTGRES_IMAGE = "postgres:18";
    private static final String POSTGRES_DATA_PATH = "/var/lib/postgresql";
    private static final String MARKER_FILE = ".sdk-database";
    private static final String READY_LOG_REGEX = ".*database system is ready to accept connections.*\\s";

    private final PostgreSQLContainer<?> postgreSQLContainer;
    private final DataBaseConfig dataBaseConfig;

    public DataBaseProvider(Path projectDir, Path targetDir, Path lunaHome, DataBaseConfig config) throws IOException {
        if (config != null) {
            dataBaseConfig = config;
            postgreSQLContainer = null;
            return;
        }

        String volumeName = getVolumeName(projectDir);
        Path marker = lunaHome.resolve(MARKER_FILE);
        DockerClient docker = DockerClientFactory.instance().client();

        boolean hasData = volumeExists(docker, volumeName);
        log.info(i18n.getString(hasData ? "database.provider.volume_reused" : "database.provider.volume_created", volumeName));

        org.apache.logging.log4j.Logger dataBaseLogger = LoggingUtils.getFileLogger("db", targetDir.resolve("database.log").toAbsolutePath().toString(), false);
        postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE);
        postgreSQLContainer.withReuse(false)
                           .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(dataBaseLogger.getName())))
                           .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withBinds(new Bind(volumeName, new Volume(POSTGRES_DATA_PATH))))
                           // On the first start the entrypoint restarts postgres after init, so the message is printed twice
                           .waitingFor(Wait.forLogMessage(READY_LOG_REGEX, hasData ? 1 : 2).withStartupTimeout(Duration.ofMinutes(2)));
        postgreSQLContainer.start();

        Files.writeString(marker, volumeName);

        dataBaseConfig = new DataBaseConfig();
        dataBaseConfig.setHost(postgreSQLContainer.getHost());
        dataBaseConfig.setPort(postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        dataBaseConfig.setUsername(postgreSQLContainer.getUsername());
        dataBaseConfig.setPassword(postgreSQLContainer.getPassword());
        dataBaseConfig.setDatabase(postgreSQLContainer.getDatabaseName());
    }

    /**
     * @return true if luna-home was created together with the current database volume
     */
    public static boolean isPaired(Path lunaHome) {
        return Files.isRegularFile(lunaHome.resolve(MARKER_FILE));
    }

    /**
     * Removes the project database volume if it exists.
     */
    public static void resetVolume(Path projectDir) {
        removeVolume(DockerClientFactory.instance().client(), getVolumeName(projectDir));
    }

    static String getVolumeName(Path projectDir) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                                       .digest(projectDir.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
            return "luna-sdk-db-" + HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean volumeExists(DockerClient docker, String volumeName) {
        try {
            docker.inspectVolumeCmd(volumeName).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private static void removeVolume(DockerClient docker, String volumeName) {
        if (!volumeExists(docker, volumeName)) {
            return;
        }
        log.info(i18n.getString("database.provider.volume_removing", volumeName));
        try {
            docker.removeVolumeCmd(volumeName).exec();
        } catch (NotFoundException ignored) {
            // already removed
        } catch (Exception e) {
            throw new IllegalStateException(i18n.getString("database.provider.volume_remove_failed", volumeName), e);
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

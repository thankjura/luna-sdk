package ru.slie.luna.sdk.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataBaseContainer implements AutoCloseable {
    private final MongoDBContainer mongoDBContainer;
    private final String DATABASE_NAME = "luna";
    private static final Logger log = LoggerFactory.getLogger("database");

    public DataBaseContainer(Path targetDir, boolean reUse) throws IOException {
        Path mongoDataDir = targetDir.resolve("database");
        Files.createDirectories(mongoDataDir);
        org.apache.logging.log4j.Logger dataBaseLogger = LoggingUtils.getFileLogger("db", targetDir.resolve("database.log").toAbsolutePath().toString(), false);
        mongoDBContainer = new MongoDBContainer("mongo:8")
                                   .withReuse(false)
                                   .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(dataBaseLogger.getName())));
        mongoDBContainer.start();
    }

    public String getConnectionString() {
        return mongoDBContainer.getConnectionString() + "/?directConnection=true";
    }

    public String getDataBaseName() {
        return DATABASE_NAME;
    }

    @Override
    public void close() throws Exception {
        mongoDBContainer.stop();
    }
}

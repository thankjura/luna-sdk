package ru.slie.luna.sdk.utils;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataBaseProvider implements AutoCloseable {
    private final MongoDBContainer mongoDBContainer;
    private final String databaseName;
    private final String databaseUri;
    private static final Pattern URI_PATTERN = Pattern.compile("^(mongodb:(?:\\/{2})?)((\\w+?):(\\w+?)@|:?@?)(\\w+?):(\\d+)\\/(?<database>\\w+)$");
    private static final I18nResolver i18n = new I18nResolver();

    public DataBaseProvider(Path targetDir, String dbUri) throws IOException, ParseValueException {
        if (dbUri != null) {
            mongoDBContainer = null;
            databaseUri = dbUri;
            Matcher matcher = URI_PATTERN.matcher(dbUri);
            if (matcher.find()) {
                databaseName = matcher.group("database");
            } else {
                throw new ParseValueException(i18n.getString("database.provider.parse_uri_error"));
            }
        } else {
            databaseName = "luna";
            Path mongoDataDir = targetDir.resolve("database");
            Files.createDirectories(mongoDataDir);
            org.apache.logging.log4j.Logger dataBaseLogger = LoggingUtils.getFileLogger("db", targetDir.resolve("database.log").toAbsolutePath().toString(), false);
            mongoDBContainer = new MongoDBContainer("mongo:8")
                                       .withReuse(false)
                                       .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(dataBaseLogger.getName())));
            mongoDBContainer.start();
            databaseUri = mongoDBContainer.getConnectionString() + "/?directConnection=true";
        }
    }

    public String getConnectionString() {
        return databaseUri;
    }

    public String getDataBaseName() {
        return databaseName;
    }

    @Override
    public void close() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    public boolean isContainer() {
        return mongoDBContainer != null;
    }
}

package ru.slie.luna.sdk.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class LoggingUtils {
    public static org.apache.logging.log4j.Logger getFileLogger(String loggerName, String filePath, boolean keepStd) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        PatternLayout layout = PatternLayout.newBuilder()
                                       .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                                       .build();

        FileAppender appender = FileAppender.newBuilder()
                                        .setConfiguration(config)
                                        .withFileName(filePath)
                                        .withAppend(true)
                                        .setName(loggerName)
                                        .setLayout(layout)
                                        .build();
        appender.start();

        config.addAppender(appender);

        Logger logger = (Logger) LogManager.getLogger(loggerName);
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(keepStd);

        return logger;
    }
}

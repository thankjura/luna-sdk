package ru.slie.luna.sdk.command;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MongoDBContainer;
import picocli.CommandLine;
import ru.slie.luna.sdk.utils.CargoUtils;
import ru.slie.luna.sdk.utils.I18nResolver;
import ru.slie.luna.sdk.utils.ProjectUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(name = "run", description = "${command.run.description}", mixinStandardHelpOptions = true)
public class RunCommand implements Runnable {
    private final I18nResolver i18n = new I18nResolver();
    private final Logger log = LoggerFactory.getLogger("luna-sdk");

    @CommandLine.Option(names = {"-j", "--jvmargs"}, descriptionKey = "command.run.option.jvmargs")
    private String jvmArgs;

    @CommandLine.Option(names = {"-c", "--reuse"}, descriptionKey = "command.run.option.reuse")
    private boolean reUse = true;

    @Override public void run() {
        Path currentDirPath = Paths.get("").toAbsolutePath();
        new ProjectUtils().checkProjectRoot(currentDirPath);
        Path sdkHome = Path.of(System.getProperty("luna.sdk.home"));
        if (!Files.isDirectory(sdkHome)) {
            throw new RuntimeException();
        }

        log.info(i18n.getString("command.run.cargo.prepare_run"));

        //String hostDataBasePath = currentDirPath.resolve("target").resolve("mongo").toAbsolutePath().toString();
        Path lunaHome = currentDirPath.resolve("target").resolve("luna-home").toAbsolutePath();
        try {
            Files.createDirectories(lunaHome);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (MongoDBContainer mongo = new MongoDBContainer("mongo:8")
                                                         .withReuse(reUse)) {

            mongo.start();
            String uri = mongo.getReplicaSetUrl("luna");
            String database = "luna";

            log.info(i18n.getString("command.run.mongo.run"), uri);

            try {
                Files.writeString(lunaHome.resolve("database.yml"), String.format("uri: %s\ndatabase: %s", uri, database));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            log.info(i18n.getString("command.run.cargo.run"));

            try {
                Path warPath = sdkHome.resolve("repository/ru/slie/luna/luna-web/1.0.0/luna-web-1.0.0.war");

                InstalledLocalContainer container = CargoUtils.makeContainer(currentDirPath.resolve("target").toAbsolutePath(), warPath.toAbsolutePath(), jvmArgs);
                container.start();

                log.info(i18n.getString("command.run.cargo.tomcat_started", " http://localhost:7080"));

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        log.info(i18n.getString("command.run.cargo.tomcat_stopping"));
                        container.stop();
                        log.info(i18n.getString("command.run.cargo.tomcat_stopped"));
                    } catch (Exception e) {
                        log.error(i18n.getString("command.run.cargo.tomcat_stopping_error"), e);
                    }
                }));

                Thread.currentThread().join();

            } catch (Exception e) {
                log.error(i18n.getString("command.run.cargo.failed_to_start"));
                throw new RuntimeException(e);
            }
        }
    }
}

package ru.slie.luna.sdk.command;

import org.apache.maven.model.Model;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import ru.slie.luna.sdk.utils.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.slie.luna.sdk.utils.MavenRepositoryHelper.getArtifact;
import static ru.slie.luna.sdk.utils.ProjectUtils.getModel;

@CommandLine.Command(name = "run", description = "${command.run.description}", mixinStandardHelpOptions = true)
public class RunCommand implements Runnable {
    private final I18nResolver i18n = new I18nResolver();
    private final Logger log = LoggerFactory.getLogger("luna-sdk");

    @CommandLine.Option(names = {"-j", "--jvmargs"}, descriptionKey = "command.run.option.jvmargs", arity = "1")
    private String jvmArgs;

    @CommandLine.Option(names = {"-p", "--port"}, descriptionKey = "command.run.option.listen_port")
    private int port = 7080;

    @CommandLine.Option(names = {"--host"}, descriptionKey = "command.run.option.listen_host")
    private String host = "localhost";

    @CommandLine.Option(names = {"-d", "--db-uri"}, descriptionKey = "command.run.option.db_uri")
    private String dbUri;

    @CommandLine.Option(names = {"-a", "--attach"}, descriptionKey = "command.run.option.attach_logs", arity = "0")
    private Boolean attach = false;

    @Override
    public void run() {
        new ProjectPackageCommand().run();
        Path currentDirPath = Paths.get("").toAbsolutePath();
        new ProjectUtils().checkProjectRoot(currentDirPath);
        Path sdkHome = Path.of(System.getProperty("luna.sdk.home"));
        if (!Files.isDirectory(sdkHome)) {
            throw new RuntimeException();
        }

        log.info(i18n.getString("command.run.cargo.prepare_run"));

        Path targetDir = currentDirPath.resolve("target");
        Path lunaHome = currentDirPath.resolve("target").resolve("luna-home").toAbsolutePath();
        Model model;

        try {
            Files.createDirectories(lunaHome);
            model = getModel(currentDirPath);
        } catch (Exception e) {
            throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
        }

        if (!model.getProperties().containsKey("luna.version")) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.please_define_luna_version"));
        }


        Artifact artifact = new DefaultArtifact("ru.slie.luna", "luna-web", "war", model.getProperties().getProperty("luna.version"));
        ArtifactResult result;

        try {
            result = getArtifact(artifact);
        } catch (ArtifactResolutionException e) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.failed_to_get_luna", artifact));
        }
        Path warPath = result.getArtifact().getFile().toPath();

        DataBaseConfig config = null;

        if (dbUri != null) {
            config = new DataBaseConfig(dbUri);
        }

        try (DataBaseProvider dataBaseProvider = new DataBaseProvider(targetDir, config)) {
            config = dataBaseProvider.getConfig();

            log.info(i18n.getString("command.run.database.run", config.getDbUri()));

            try {
                Files.writeString(lunaHome.resolve("database.yml"), config.toYamlConfig());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            log.info(i18n.getString("command.run.cargo.run"));

            try {
                Path pluginsDir = sdkHome.resolve("plugins");

                InstalledLocalContainer container = CargoUtils.makeContainer(currentDirPath.resolve("target"), pluginsDir, warPath, jvmArgs, host, port);

                if (attach) {
                    SLF4JBridgeHandler.removeHandlersForRootLogger();
                    SLF4JBridgeHandler.install();
                }

                container.start();
                log.info(container.getHome());

                log.info(i18n.getString("command.run.cargo.tomcat_started", String.format("http://%s:%s", host, port)));

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        log.info(i18n.getString("command.run.cargo.tomcat_stopping"));
                        container.stop();
                        log.info(i18n.getString("command.run.cargo.tomcat_stopped"));
                    } catch (Exception e) {
                        log.error(i18n.getString("command.run.cargo.tomcat_stopping_error"), e);
                    }

                    if (dataBaseProvider.isContainer()) {
                        try {
                            log.info(i18n.getString("command.run.cargo.database_stopping"));
                            dataBaseProvider.close();
                            log.info(i18n.getString("command.run.cargo.database_stopped"));
                        } catch (Exception e) {
                            log.error(i18n.getString("command.run.cargo.database_stopping_error"), e);
                        }
                    }
                }));

                Thread.currentThread().join();

            } catch (Exception e) {
                log.error(i18n.getString("command.run.cargo.failed_to_start"));
                throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
            }
        } catch (Exception e) {
            throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
        }
    }
}

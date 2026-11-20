package ru.slie.luna.sdk.command;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.maven.model.Model;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import ru.slie.luna.sdk.utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static ru.slie.luna.sdk.utils.MavenRepositoryHelper.getArtifact;
import static ru.slie.luna.sdk.utils.ProjectUtils.getModel;

@CommandLine.Command(name = "run", description = "${command.run.description}", mixinStandardHelpOptions = true)
public class RunCommand implements Runnable {
    private static final String TOMCAT_MAIN_CLASS = "org.apache.catalina.startup.Bootstrap";

    private final I18nResolver i18n = new I18nResolver();
    private final Logger log = LoggerFactory.getLogger("luna-sdk");
    private final Logger tomcatOutput = LoggerFactory.getLogger("tomcat.output");

    @CommandLine.Option(names = {"-j", "--jvmargs"}, descriptionKey = "command.run.option.jvmargs", arity = "1")
    private String jvmArgs;

    @CommandLine.Option(names = {"-p", "--port"}, descriptionKey = "command.run.option.listen_port")
    private int port = 7080;

    @CommandLine.Option(names = {"--host"}, descriptionKey = "command.run.option.listen_host")
    private String host = "localhost";

    @CommandLine.Option(names = {"-d", "--db-uri"}, descriptionKey = "command.run.option.db_uri")
    private String dbUri;

    @CommandLine.Option(names = {"-a", "--attach"}, descriptionKey = "command.run.option.attach_logs", arity = "0")
    private boolean attach = false;

    @CommandLine.Option(names = {"--debug"}, descriptionKey = "command.run.option.debug", arity = "0..1", fallbackValue = "5005", paramLabel = "PORT")
    private Integer debugPort;

    @CommandLine.Option(names = {"--timeout"}, descriptionKey = "command.run.option.timeout", paramLabel = "SECONDS")
    private int timeoutSeconds = 300;

    @CommandLine.Option(names = {"--no-build"}, descriptionKey = "command.run.option.no_build")
    private boolean noBuild = false;

    @CommandLine.Option(names = {"-o", "--offline"}, descriptionKey = "command.option.offline")
    private boolean offline = false;

    @CommandLine.Option(names = {"--reset"}, descriptionKey = "command.run.option.reset")
    private boolean reset = false;

    private final AtomicReference<InstalledLocalContainer> containerRef = new AtomicReference<>();
    private final AtomicReference<DataBaseProvider> dataBaseRef = new AtomicReference<>();
    private final AtomicBoolean tomcatStarted = new AtomicBoolean(false);
    private final AtomicBoolean tomcatExited = new AtomicBoolean(false);
    private final AtomicBoolean shutdownDone = new AtomicBoolean(false);

    @Override
    public void run() {
        Path projectDir = Paths.get("").toAbsolutePath();
        new ProjectUtils().checkProjectRoot(projectDir);

        String sdkHomeProperty = System.getProperty("luna.sdk.home");
        if (sdkHomeProperty == null || !Files.isDirectory(Path.of(sdkHomeProperty))) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.sdk_home_not_defined"));
        }
        Path sdkHome = Path.of(sdkHomeProperty);

        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        checkPortFree(port);
        if (debugPort != null) {
            checkPortFree(debugPort);
        }

        if (!noBuild) {
            new PackageCommand(offline).run();
        }

        log.info(i18n.getString("command.run.cargo.prepare_run"));

        Path targetDir = projectDir.resolve("target");
        Path lunaHome = targetDir.resolve("luna-home").toAbsolutePath();
        Model model;
        try {
            model = getModel(projectDir);
        } catch (Exception e) {
            throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
        }

        String lunaVersion = model.getProperties().getProperty("luna.version");
        if (lunaVersion == null) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.please_define_luna_version"));
        }

        Path warPath = resolveLuna(lunaVersion);
        DataBaseConfig config;
        try {
            config = dbUri != null ? new DataBaseConfig(dbUri) : null;
        } catch (IllegalArgumentException e) {
            throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "luna-sdk-shutdown"));

        try {
            // luna-home without a paired container database belongs to a lost database: start from scratch
            boolean unpaired = config == null && !DataBaseProvider.isPaired(lunaHome);
            if (reset || unpaired) {
                if (Files.exists(lunaHome)) {
                    log.info(i18n.getString("command.run.reset", lunaHome.toString()));
                    TomcatInstaller.deleteRecursively(lunaHome);
                }
                if (config == null) {
                    DataBaseProvider.resetVolume(projectDir);
                }
            }
            Files.createDirectories(lunaHome);
            copyLicense(lunaHome);
            CargoUtils.installSdkPlugins(sdkHome.resolve("plugins"), lunaHome);
            Path tomcatHome = TomcatInstaller.install(targetDir, offline);

            DataBaseProvider dataBaseProvider = new DataBaseProvider(projectDir, targetDir, lunaHome, config);
            dataBaseRef.set(dataBaseProvider);
            config = dataBaseProvider.getConfig();
            log.info(i18n.getString("command.run.database.run", config.getDisplayUri()));
            Files.writeString(lunaHome.resolve("database.yml"), config.toYamlConfig());

            log.info(i18n.getString("command.run.cargo.run"));
            InstalledLocalContainer container = CargoUtils.makeContainer(targetDir, tomcatHome, warPath, jvmArgs, host, port,
                                                                         debugPort, Duration.ofSeconds(timeoutSeconds).toMillis());
            containerRef.set(container);

            Path outputLog = CargoUtils.getOutputLog(targetDir);
            Files.deleteIfExists(outputLog);
            if (attach) {
                attachOutput(outputLog);
            } else {
                log.info(i18n.getString("command.run.cargo.output_log", outputLog.toString()));
            }

            container.start();
            tomcatStarted.set(true);
            log.info(i18n.getString("command.run.cargo.tomcat_started", String.format("http://%s:%s", host, port)));
            if (debugPort != null) {
                log.info(i18n.getString("command.run.cargo.debug_listening", String.format("%s:%d", host, debugPort)));
            }
        } catch (CommandLine.PicocliException e) {
            throw e;
        } catch (Exception e) {
            log.error(i18n.getString("command.run.cargo.failed_to_start"));
            throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
        }

        waitForTomcatExit();
    }

    private Path resolveLuna(String lunaVersion) {
        Artifact artifact = new DefaultArtifact("ru.slie.luna", "luna-web", "war", lunaVersion);
        try {
            return getArtifact(artifact, offline).getArtifact().getFile().toPath();
        } catch (ArtifactResolutionException e) {
            String message = i18n.getString("command.run.failed_to_get_luna", artifact.toString());
            if (artifact.isSnapshot()) {
                message += System.lineSeparator() + i18n.getString("command.run.snapshot_hint");
            }
            throw new CommandLine.PicocliException(message, e);
        }
    }

    private void copyLicense(Path lunaHome) {
        Path license = lunaHome.resolve("luna.license");
        if (Files.exists(license)) {
            return;
        }
        log.info(i18n.getString("command.run.copy_license"));
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("luna.license")) {
            if (is != null) {
                Files.copy(is, license);
            }
        } catch (IOException e) {
            log.error(i18n.getString("command.run.failed_copy_license"), e);
        }
    }

    private void checkPortFree(int portToCheck) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(host, portToCheck));
        } catch (IOException e) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.port_busy", String.format("%s:%d", host, portToCheck)));
        }
    }

    private void attachOutput(Path outputLog) {
        Tailer.builder()
              .setPath(outputLog)
              .setCharset(StandardCharsets.UTF_8)
              .setDelayDuration(Duration.ofMillis(300))
              .setTailFromEnd(false)
              .setTailerListener(new TailerListenerAdapter() {
                  @Override
                  public void handle(String line) {
                      tomcatOutput.info(line);
                  }
              })
              .get();
    }

    /**
     * Blocks until the Tomcat JVM started by Cargo exits. If it exits on its own (crash, OOM, killed),
     * the database is stopped and the command fails instead of hanging forever.
     */
    private void waitForTomcatExit() {
        Optional<ProcessHandle> tomcatProcess = findTomcatProcess();
        if (tomcatProcess.isEmpty()) {
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        tomcatProcess.get().onExit().join();
        try {
            // On Ctrl+C Cargo's own shutdown hook may kill Tomcat before ours marks the shutdown
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (shutdownDone.get()) {
            return;
        }
        tomcatExited.set(true);
        throw new CommandLine.PicocliException(i18n.getString("command.run.cargo.tomcat_exited_unexpectedly"));
    }

    private Optional<ProcessHandle> findTomcatProcess() {
        List<ProcessHandle> children = ProcessHandle.current().children().filter(ProcessHandle::isAlive).toList();
        Optional<ProcessHandle> byCommand = children.stream()
                                                    .filter(p -> p.info().commandLine().map(c -> c.contains(TOMCAT_MAIN_CLASS)).orElse(false)
                                                            || p.info().arguments().map(a -> List.of(a).contains(TOMCAT_MAIN_CLASS)).orElse(false))
                                                    .findFirst();
        if (byCommand.isPresent() || children.size() != 1) {
            return byCommand;
        }
        return Optional.of(children.getFirst());
    }

    private void shutdown() {
        if (!shutdownDone.compareAndSet(false, true)) {
            return;
        }

        InstalledLocalContainer container = containerRef.get();
        if (container != null && tomcatStarted.get() && !tomcatExited.get()) {
            try {
                log.info(i18n.getString("command.run.cargo.tomcat_stopping"));
                container.stop();
                log.info(i18n.getString("command.run.cargo.tomcat_stopped"));
            } catch (Exception e) {
                log.error(i18n.getString("command.run.cargo.tomcat_stopping_error"), e);
            }
        }

        DataBaseProvider dataBaseProvider = dataBaseRef.get();
        if (dataBaseProvider != null && dataBaseProvider.isContainer()) {
            try {
                log.info(i18n.getString("command.run.cargo.database_stopping"));
                dataBaseProvider.close();
                log.info(i18n.getString("command.run.cargo.database_stopped"));
            } catch (Exception e) {
                log.error(i18n.getString("command.run.cargo.database_stopping_error"), e);
            }
        }
    }
}

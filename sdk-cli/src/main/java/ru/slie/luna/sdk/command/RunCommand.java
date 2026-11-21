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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @CommandLine.Option(names = {"-w", "--workdir"}, descriptionKey = "command.run.option.workdir", paramLabel = "DIR")
    private Path workDir;

    @CommandLine.Option(names = {"--luna-version"}, descriptionKey = "command.run.option.luna_version", paramLabel = "VERSION")
    private String lunaVersionOption;

    @CommandLine.Option(names = {"--no-plugin"}, descriptionKey = "command.run.option.no_plugin")
    private boolean noPlugin = false;

    @CommandLine.Option(names = {"--cluster"}, descriptionKey = "command.run.option.cluster")
    private boolean cluster = false;

    @CommandLine.Option(names = {"--shared-home"}, descriptionKey = "command.run.option.shared_home", paramLabel = "DIR")
    private Path sharedHome;

    @CommandLine.Option(names = {"--node-id"}, descriptionKey = "command.run.option.node_id", paramLabel = "ID")
    private String nodeId;

    private final AtomicReference<InstalledLocalContainer> containerRef = new AtomicReference<>();
    private final AtomicReference<DataBaseProvider> dataBaseRef = new AtomicReference<>();
    private final AtomicBoolean tomcatStarted = new AtomicBoolean(false);
    private final AtomicBoolean tomcatExited = new AtomicBoolean(false);
    private final AtomicBoolean shutdownDone = new AtomicBoolean(false);

    @Override
    public void run() {
        Path currentDir = Paths.get("").toAbsolutePath();
        boolean pluginMode = !noPlugin && new ProjectUtils().isPluginProject(currentDir);
        if (!pluginMode && workDir == null) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.workdir_required"));
        }

        String sdkHomeProperty = System.getProperty("luna.sdk.home");
        if (sdkHomeProperty == null || !Files.isDirectory(Path.of(sdkHomeProperty))) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.sdk_home_not_defined"));
        }
        Path sdkHome = Path.of(sdkHomeProperty);

        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (cluster && dbUri == null) {
            // a database container belongs to one instance: nodes of a cluster must share one database
            throw new CommandLine.PicocliException(i18n.getString("command.run.cluster_db_required"));
        }
        checkPortFree(port);
        if (debugPort != null) {
            checkPortFree(debugPort);
        }

        // Instance state (luna-home, database, tomcat) lives in --workdir or in the plugin target directory
        Path stateDir = (workDir != null ? workDir : currentDir.resolve("target")).toAbsolutePath().normalize();
        Path lunaHome = stateDir.resolve("luna-home");
        Path databaseDir = stateDir.resolve("database");
        // quick-reload watches the plugin build output, only in plugin mode
        Path quickReloadDir = pluginMode ? currentDir.resolve("target") : null;
        String lunaVersion = resolveLunaVersion(currentDir, pluginMode);

        if (pluginMode && !noBuild) {
            new PackageCommand(offline).run();
        }

        log.info(i18n.getString(pluginMode ? "command.run.mode.plugin" : "command.run.mode.standalone", stateDir.toString(), lunaVersion));
        log.info(i18n.getString("command.run.cargo.prepare_run"));

        Path warPath = resolveLuna(lunaVersion);
        DataBaseConfig config;
        try {
            config = dbUri != null ? new DataBaseConfig(dbUri) : null;
        } catch (IllegalArgumentException e) {
            throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "luna-sdk-shutdown"));

        try {
            if (reset) {
                log.info(i18n.getString("command.run.reset", stateDir.toString()));
                TomcatInstaller.deleteRecursively(lunaHome);
                if (config == null) {
                    TomcatInstaller.deleteRecursively(databaseDir);
                }
            }
            Files.createDirectories(lunaHome);
            copyLicense(lunaHome);
            Path shared = sharedHome != null ? sharedHome.toAbsolutePath().normalize() : null;
            if (shared != null) {
                Files.createDirectories(shared);
            }
            CargoUtils.installSdkPlugins(sdkHome.resolve("plugins"), pluginsHome(lunaHome, shared, lunaVersion), lunaHome);
            Path tomcatHome = TomcatInstaller.install(stateDir, offline);

            DataBaseProvider dataBaseProvider = new DataBaseProvider(stateDir, databaseDir, config);
            dataBaseRef.set(dataBaseProvider);
            config = dataBaseProvider.getConfig();
            log.info(i18n.getString("command.run.database.run", config.getDisplayUri()));
            Path databaseYml = lunaHome.resolve("database.yml");
            Files.writeString(databaseYml, config.toYamlConfig(Files.exists(databaseYml) ? Files.readString(databaseYml) : null));

            log.info(i18n.getString("command.run.cargo.run"));
            Map<String, String> properties = new LinkedHashMap<>();
            if (cluster) {
                properties.put("luna.cluster", "true");
                log.info(i18n.getString("command.run.cluster", nodeId != null ? nodeId : "-",
                        shared != null ? shared.toString() : lunaHome.resolve("shared").toString()));
            }
            if (shared != null) {
                properties.put("luna.shared.home", shared.toString());
            }
            if (nodeId != null && !nodeId.isBlank()) {
                properties.put("luna.node.id", nodeId);
            }
            InstalledLocalContainer container = CargoUtils.makeContainer(stateDir, lunaHome, quickReloadDir, tomcatHome, warPath,
                                                                         jvmArgs, properties, host, port, debugPort,
                                                                         Duration.ofSeconds(timeoutSeconds).toMillis());
            containerRef.set(container);

            Path outputLog = CargoUtils.getOutputLog(stateDir);
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

    /**
     * Home that holds installed plugins: Luna 4 keeps them in the shared home ({@code luna-home/shared} unless
     * {@code --shared-home} is given), older versions in luna-home.
     */
    static Path pluginsHome(Path lunaHome, Path sharedHome, String lunaVersion) {
        if (sharedHome != null) {
            return sharedHome;
        }
        return majorVersion(lunaVersion) >= 4 ? lunaHome.resolve("shared") : lunaHome;
    }

    static int majorVersion(String version) {
        if (version == null) {
            return 0;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)").matcher(version.trim());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String resolveLunaVersion(Path projectDir, boolean pluginMode) {
        if (lunaVersionOption != null && !lunaVersionOption.isBlank()) {
            return lunaVersionOption;
        }
        if (!pluginMode) {
            return SdkProperties.get("luna.version");
        }
        Model model;
        try {
            model = getModel(projectDir);
        } catch (Exception e) {
            throw new CommandLine.PicocliException(e.getLocalizedMessage(), e);
        }
        String version = model.getProperties().getProperty("luna.version");
        if (version == null) {
            throw new CommandLine.PicocliException(i18n.getString("command.run.please_define_luna_version"));
        }
        return version;
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
            // as Tomcat does: connections left in TIME_WAIT by clients must not make a free port look busy,
            // a port someone listens on still fails to bind
            socket.setReuseAddress(true);
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

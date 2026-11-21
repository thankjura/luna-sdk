package ru.slie.luna.sdk.utils;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat11xInstalledLocalContainer;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat11xStandaloneLocalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class CargoUtils {
    private static final Logger log = LoggerFactory.getLogger("tomcat");
    private static final I18nResolver i18n = new I18nResolver();
    private static final String SDK_PLUGINS_MANIFEST = ".sdk-plugins";

    private CargoUtils() {
    }

    public static Path getOutputLog(Path stateDir) {
        return stateDir.resolve("tomcat-base").resolve("logs").resolve("application.log");
    }

    /**
     * @param quickReloadDir plugin build directory watched by quick-reload, {@code null} to run without a plugin
     */
    public static InstalledLocalContainer makeContainer(Path stateDir, Path lunaHome, Path quickReloadDir, Path tomcatHome,
                                                        Path warPath, String jvmArgs, Map<String, String> systemProperties,
                                                        String host, int port, Integer debugPort, long timeoutMs) {
        Tomcat11xStandaloneLocalConfiguration configuration = new Tomcat11xStandaloneLocalConfiguration(stateDir.resolve("tomcat-base").toString());
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(port));
        // shutdown and AJP ports follow the HTTP port, so instances with different --port can run side by side;
        // the default port 7080 keeps the Cargo defaults 8205 and 8009
        configuration.setProperty(GeneralPropertySet.RMI_PORT, String.valueOf(port + 1125));
        configuration.setProperty(TomcatPropertySet.AJP_PORT, String.valueOf(port + 929));
        configuration.setProperty(GeneralPropertySet.HOSTNAME, host);

        StringBuilder args = new StringBuilder(String.format("-Dluna.home=\"%s\"", lunaHome));
        if (quickReloadDir != null) {
            args.append(String.format(" -Dquickreload.dir=\"%s\"", quickReloadDir));
        }
        systemProperties.forEach((key, value) -> args.append(String.format(" -D%s=\"%s\"", key, value)));
        if (debugPort != null) {
            args.append(String.format(" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%s:%d", host, debugPort));
        }
        if (jvmArgs != null && !jvmArgs.isBlank()) {
            args.append(' ').append(jvmArgs);
        }
        configuration.setProperty(GeneralPropertySet.JVMARGS, args.toString());

        log.info(i18n.getString("cargo.installer.upload_war", warPath.toString()));
        WAR mainWar = new WAR(warPath.toString());
        mainWar.setContext("/");
        configuration.addDeployable(mainWar);

        Tomcat11xInstalledLocalContainer container = new Tomcat11xInstalledLocalContainer(configuration);
        log.info(i18n.getString("cargo.installer.home", tomcatHome.toString()));
        container.setHome(tomcatHome.toString());
        container.setTimeout(timeoutMs);
        container.setOutput(getOutputLog(stateDir).toString());
        return container;
    }

    /**
     * Copies SDK bundled plugins (quick-reload, ...) into {@code <pluginsHome>/plugins/installed} and removes the
     * ones copied by a previous SDK version, so an SDK upgrade does not leave two versions of the same plugin
     * installed.
     *
     * @param pluginsHome the home that holds installed plugins (the shared home of Luna 4)
     * @param lunaHome    the home of this instance, keeps the list of copied plugins
     */
    public static void installSdkPlugins(Path pluginsDir, Path pluginsHome, Path lunaHome) throws IOException {
        Path installedDir = pluginsHome.resolve("plugins").resolve("installed");
        Path manifest = lunaHome.resolve(SDK_PLUGINS_MANIFEST);
        Files.createDirectories(installedDir);

        Set<String> previous = Files.isRegularFile(manifest) ? new HashSet<>(Files.readAllLines(manifest)) : Set.of();
        List<String> current = new ArrayList<>();

        if (Files.isDirectory(pluginsDir)) {
            try (Stream<Path> files = Files.list(pluginsDir)) {
                for (Path f : files.filter(p -> p.getFileName().toString().endsWith(".jar")).toList()) {
                    String name = f.getFileName().toString();
                    log.info(i18n.getString("cargo.installer.copy_plugin", name));
                    Files.copy(f, installedDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    current.add(name);
                }
            }
        }

        for (String name : previous) {
            if (!name.isBlank() && !current.contains(name)) {
                log.info(i18n.getString("cargo.installer.remove_plugin", name));
                Files.deleteIfExists(installedDir.resolve(name));
            }
        }

        Files.write(manifest, current);
    }
}

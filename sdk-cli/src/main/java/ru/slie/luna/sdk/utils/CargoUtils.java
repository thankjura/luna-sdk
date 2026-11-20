package ru.slie.luna.sdk.utils;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat11xInstalledLocalContainer;
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
import java.util.Set;
import java.util.stream.Stream;

public class CargoUtils {
    private static final Logger log = LoggerFactory.getLogger("tomcat");
    private static final I18nResolver i18n = new I18nResolver();
    private static final String SDK_PLUGINS_MANIFEST = ".sdk-plugins";

    private CargoUtils() {
    }

    public static Path getOutputLog(Path targetDir) {
        return targetDir.resolve("tomcat-base").resolve("logs").resolve("application.log");
    }

    public static InstalledLocalContainer makeContainer(Path targetDir, Path tomcatHome, Path warPath, String jvmArgs,
                                                        String host, int port, Integer debugPort, long timeoutMs) {
        Tomcat11xStandaloneLocalConfiguration configuration = new Tomcat11xStandaloneLocalConfiguration(targetDir.resolve("tomcat-base").toString());
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(port));
        configuration.setProperty(GeneralPropertySet.HOSTNAME, host);

        StringBuilder args = new StringBuilder()
                .append(String.format("-Dluna.home=\"%s\" -Dquickreload.dir=\"%s\"", targetDir.resolve("luna-home"), targetDir));
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
        container.setOutput(getOutputLog(targetDir).toString());
        return container;
    }

    /**
     * Copies SDK bundled plugins (quick-reload, ...) into luna-home and removes the ones copied by
     * a previous SDK version, so an SDK upgrade does not leave two versions of the same plugin installed.
     */
    public static void installSdkPlugins(Path pluginsDir, Path lunaHome) throws IOException {
        Path installedDir = lunaHome.resolve("plugins").resolve("installed");
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

package ru.slie.luna.sdk.utils;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.Tomcat11xInstalledLocalContainer;
import org.codehaus.cargo.container.tomcat.Tomcat11xStandaloneLocalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class CargoUtils {
    private static final Logger log = LoggerFactory.getLogger("tomcat");
    private static final I18nResolver i18n = new I18nResolver();


    public static InstalledLocalContainer makeContainer(Path targetDir, Path pluginsDir, Path warPath, String jvmArgs, String host, int port) throws IOException {
        Installer installer = new CustomZipURLInstaller(URI.create("https://archive.apache.org/dist/tomcat/tomcat-11/v11.0.18/bin/apache-tomcat-11.0.18.zip").toURL(), targetDir);
        installer.install();

        Tomcat11xStandaloneLocalConfiguration configuration = new Tomcat11xStandaloneLocalConfiguration(targetDir.resolve("tomcat-base").toString());
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(port));
        if (host == null || host.isEmpty()) {
            host = "localhost";
        }
        configuration.setProperty(GeneralPropertySet.HOSTNAME, host);


        if (jvmArgs == null) {
            jvmArgs = "";
        }

        jvmArgs = String.format("-Dluna.home=\"%s\" -Dquickreload.dir=\"%s\" %s", targetDir.resolve("luna-home"), targetDir, jvmArgs);
        configuration.setProperty(GeneralPropertySet.JVMARGS, jvmArgs);

        if (Files.isDirectory(pluginsDir)) {
            Path installedDir = targetDir.resolve("luna-home").resolve("plugins").resolve("installed");
            Files.createDirectories(installedDir);
            try (Stream<Path> files = Files.list(pluginsDir)) {
                files.forEach(f -> {
                    try {
                        log.info(i18n.getString("cargo.installer.copy_plugin", f.getFileName().toString()));
                        Files.copy(f, installedDir.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log.error(i18n.getString("cargo.installer.failed.copy_plugin", f.getFileName().toString()), e);
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        log.info(i18n.getString("cargo.installer.upload_war", warPath.toString()));
        WAR mainWar = new WAR(warPath.toString());
        mainWar.setContext("/");
        configuration.addDeployable(mainWar);

        Tomcat11xInstalledLocalContainer container = new Tomcat11xInstalledLocalContainer(configuration);
        log.info(i18n.getString("cargo.installer.home", installer.getHome()));
        container.setHome(installer.getHome());
        container.setOutput(targetDir.resolve("tomcat-base").resolve("logs").resolve("application.log").toString());
        return container;
    }
}

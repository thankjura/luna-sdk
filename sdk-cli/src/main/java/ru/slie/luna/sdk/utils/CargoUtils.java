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

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;

public class CargoUtils {
    private static final Logger log = LoggerFactory.getLogger("tomcat");
    private static final I18nResolver i18n = new I18nResolver();


    public static InstalledLocalContainer makeContainer(Path targetDir, Path pluginsDir, Path warPath, String jvmArgs) throws MalformedURLException {
        Installer installer = new CustomZipURLInstaller(URI.create("https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.15/bin/apache-tomcat-11.0.15.zip").toURL(), targetDir);
        installer.install();

        Tomcat11xStandaloneLocalConfiguration configuration = new Tomcat11xStandaloneLocalConfiguration(targetDir.resolve("tomcat-base").toString());
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(7080));


        if (jvmArgs == null) {
            jvmArgs = "";
        } else {
            jvmArgs += " ";
        }

        jvmArgs += "-Dluna.home=\"" + targetDir.resolve("luna-home") + "\"";
        configuration.setProperty(GeneralPropertySet.JVMARGS, jvmArgs);

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

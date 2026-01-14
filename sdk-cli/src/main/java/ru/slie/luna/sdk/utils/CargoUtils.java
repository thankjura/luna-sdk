package ru.slie.luna.sdk.utils;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;

public class CargoUtils {
    private static final String CONTAINER_ID = "tomcat11x";

    public static InstalledLocalContainer makeContainer(Path targetDir, Path warPath, String jvmArgs) throws MalformedURLException {
        Installer installer = new CustomZipURLInstaller(URI.create("https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.15/bin/apache-tomcat-11.0.15.zip").toURL(), targetDir);
        installer.install();

        LocalConfiguration configuration = (LocalConfiguration) new DefaultConfigurationFactory().createConfiguration(CONTAINER_ID, ContainerType.INSTALLED, ConfigurationType.STANDALONE);
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(7080));
        if (jvmArgs == null) {
            jvmArgs = "";
        } else {
            jvmArgs += " ";
        }

        jvmArgs += "-Dluna.home=\"" + targetDir.resolve("luna-home") + "\"";
        configuration.setProperty(GeneralPropertySet.JVMARGS, jvmArgs);
        WAR mainWar = new WAR(warPath.toString());
        mainWar.setContext("/");
        configuration.addDeployable(mainWar);

        InstalledLocalContainer container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(CONTAINER_ID, ContainerType.INSTALLED, configuration);
        container.setHome(installer.getHome());
        System.out.println(installer.getHome());

        return container;
    }
}

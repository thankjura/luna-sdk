package ru.slie.luna.sdk.utils;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;

public class CargoUtils {
    private static final String CONTAINER_ID = "tomcat11x";

    public static InstalledLocalContainer makeContainer(Path targetDir, Path warPath) throws MalformedURLException {
        Installer installer = new ZipURLInstaller(URI.create("https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.15/bin/apache-tomcat-11.0.15.zip").toURL(), targetDir.resolve("downloads").toString(), targetDir.toString());
        installer.install();

        LocalConfiguration configuration = (LocalConfiguration) new DefaultConfigurationFactory().createConfiguration(CONTAINER_ID, ContainerType.INSTALLED, ConfigurationType.STANDALONE);
        configuration.setProperty(ServletPropertySet.PORT, String.valueOf(7080));
        configuration.setProperty(GeneralPropertySet.JVMARGS, "-Dluna.home=\"" + targetDir.resolve("luna-home") + "\"");
        WAR mainWar = new WAR(warPath.toString());
        mainWar.setContext("/");
        configuration.addDeployable(mainWar);

        InstalledLocalContainer container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(CONTAINER_ID, ContainerType.INSTALLED, configuration);
        container.setHome(installer.getHome());
        System.out.println(installer.getHome());

        return container;
    }
}

package ru.slie.luna.sdk.utils;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Installs Tomcat distribution resolved as a Maven artifact (org.apache.tomcat:tomcat:zip).
 * The archive is cached in the local Maven repository, so {@code mvn clean} only costs an unzip.
 */
public class TomcatInstaller {
    private static final Logger log = LoggerFactory.getLogger("installer");
    private static final I18nResolver i18n = new I18nResolver();
    private static final String SDK_PROPERTIES = "luna-sdk.properties";

    private TomcatInstaller() {
    }

    public static String getTomcatVersion() {
        Properties properties = new Properties();
        try (InputStream is = TomcatInstaller.class.getClassLoader().getResourceAsStream(SDK_PROPERTIES)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        String version = properties.getProperty("tomcat.version");
        if (version == null || version.isBlank() || version.startsWith("${")) {
            throw new IllegalStateException("tomcat.version is not defined in " + SDK_PROPERTIES);
        }
        return version;
    }

    /**
     * @return Tomcat home directory
     */
    public static Path install(Path targetDir, boolean offline) throws IOException {
        String version = getTomcatVersion();
        Path home = targetDir.resolve("apache-tomcat-" + version);

        if (Files.isRegularFile(home.resolve("lib").resolve("catalina.jar"))) {
            log.info(i18n.getString("cargo.installer.container_already_installed", home.toString()));
            return home;
        }

        DefaultArtifact artifact = new DefaultArtifact("org.apache.tomcat", "tomcat", "zip", version);
        Path zip;
        try {
            log.info(i18n.getString("cargo.installer.container_resolving", artifact.toString()));
            zip = MavenRepositoryHelper.getArtifact(artifact, offline).getArtifact().getFile().toPath();
        } catch (ArtifactResolutionException e) {
            throw new IOException(i18n.getString("cargo.installer.container_resolve_failed", artifact.toString()), e);
        }

        log.info(i18n.getString("cargo.installer.container_installing", zip.toString(), home.toString()));
        Path tmpDir = targetDir.resolve("apache-tomcat-" + version + ".tmp");
        deleteRecursively(tmpDir);
        deleteRecursively(home);
        unzip(zip, tmpDir);

        Path extracted;
        try (var children = Files.list(tmpDir)) {
            extracted = children.filter(Files::isDirectory).findFirst()
                                .orElseThrow(() -> new IOException("Unexpected Tomcat archive layout: " + zip));
        }
        Files.move(extracted, home, StandardCopyOption.ATOMIC_MOVE);
        deleteRecursively(tmpDir);
        return home;
    }

    private static void unzip(Path zip, Path destination) throws IOException {
        Path root = destination.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}

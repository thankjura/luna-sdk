package ru.slie.luna.sdk.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class ProjectUtils {
    I18nResolver i18n = new I18nResolver();
    List<String> YAML_CONFIG = Arrays.asList("luna-plugin.yaml", "luna-plugin.yml");

    public void checkProjectRoot(Path path) {
        Path pomPath = path.resolve("pom.xml");
        if (!Files.isRegularFile(pomPath)) {
            throw new CommandLine.PicocliException(i18n.getString("project.error.is_not_plugin_directory"));
        }

        if (YAML_CONFIG.stream().anyMatch(f -> Files.isRegularFile(path.resolve("src").resolve("main").resolve("resources").resolve(f)))) {
            return;
        }

        throw new CommandLine.PicocliException(i18n.getString("project.error.is_not_plugin_directory"));
    }

    public static Model getModel(Path projectDir) throws IOException, XmlPullParserException {
        Path pomPath = projectDir.resolve("pom.xml");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (InputStream stream = Files.newInputStream(pomPath, StandardOpenOption.READ)) {
            return reader.read(stream);
        }
    }
}

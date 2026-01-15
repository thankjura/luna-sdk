package ru.slie.luna.sdk.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ProjectUtils {
    I18nResolver i18n = new I18nResolver();
    List<String> YAML_CONFIG = Arrays.asList("luna-plugin.yaml", "luna-plugin.yml");

    public void checkProjectRoot(Path path) {
        Path pomPath = path.resolve("pom.xml");
        if (!Files.isRegularFile(pomPath)) {
            throw new RuntimeException(i18n.getString("project.error.is_not_plugin_directory"));
        }

        if (YAML_CONFIG.stream().anyMatch(f -> Files.isRegularFile(path.resolve("src").resolve("main").resolve("resources").resolve(f)))) {
            return;
        }

        throw new RuntimeException(i18n.getString("project.error.is_not_plugin_directory"));
    }
}

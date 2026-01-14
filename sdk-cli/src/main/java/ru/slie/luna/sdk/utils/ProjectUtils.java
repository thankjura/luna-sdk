package ru.slie.luna.sdk.utils;

import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectUtils {
    I18nResolver i18n = new I18nResolver();

    public void checkProjectRoot(Path path) {
        Path pomPath = path.resolve("pom.xml");
        if (!Files.isRegularFile(pomPath)) {
            throw new RuntimeException(i18n.getString("project.error.is_not_plugin_directory"));
        }

        if (!Files.isRegularFile(path.resolve("src").resolve("main").resolve("resources").resolve("luna-plugin.json"))) {
            throw new RuntimeException(i18n.getString("project.error.is_not_plugin_directory"));
        }
    }
}

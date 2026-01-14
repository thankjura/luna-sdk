package ru.slie.luna.sdk.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class ProjectUtils {
    private final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    public void checkProjectRoot(Path path) {
        Path pomPath = path.resolve("pom.xml");
        if (!Files.isRegularFile(pomPath)) {
            throw new RuntimeException(bundle.getString("project.error.is_not_plugin_directory"));
        }

        if (!Files.isRegularFile(path.resolve("src").resolve("main").resolve("resources").resolve("luna-plugin.json"))) {
            throw new RuntimeException(bundle.getString("project.error.is_not_plugin_directory"));
        }
    }
}

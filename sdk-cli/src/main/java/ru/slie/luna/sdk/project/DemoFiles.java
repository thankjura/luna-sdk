package ru.slie.luna.sdk.project;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Stream;

public class DemoFiles {
    private final static String folderPath = "/templates/demo/";
    private final static String DEMO_PACKAGE = "ru.slie.luna.demo";

    public void copyDemoFiles(Path targetDir, String targetPackage) throws URISyntaxException, IOException {
        URL resource = getClass().getResource(folderPath);
        if (resource == null) {
            return;
        }

        Path sourceDir = targetDir.resolve("src").resolve("main").resolve("java");
        for (String part: targetPackage.split("\\.")) {
            sourceDir = sourceDir.resolve(part.replaceAll("-", "_"));
        }
        Path resourceDir = targetDir.resolve("src").resolve("main").resolve("resources");
        Files.createDirectories(sourceDir);
        Files.createDirectories(resourceDir);

        URI uri = resource.toURI();
        try (FileSystem ignore = uri.getScheme().equals("jar")? FileSystems.newFileSystem(uri, Collections.emptyMap()): null) {
            Path dirPath = Paths.get(uri);
            try (Stream<Path> stream = Files.walk(dirPath, 1)) {
                for (Path file: stream.toList()) {
                    if (!Files.isRegularFile(file)) {
                        continue;
                    }

                    String fileName = file.getFileName().toString();
                    String content = Files.readString(file);
                    content = content.replaceAll(DEMO_PACKAGE, targetPackage);
                    Path targetPath = sourceDir.resolve(fileName);
                    Files.writeString(targetPath, content, StandardCharsets.UTF_8);
                }
            }
        }




        try (InputStream stream = getClass().getResourceAsStream("/luna-plugin-example.json")) {
            if (stream != null) {
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                content = content.replaceAll(DEMO_PACKAGE, targetPackage);
                Files.writeString(resourceDir.resolve("luna-plugin.json"), content, StandardCharsets.UTF_8);
            }
        }
    }
}

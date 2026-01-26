package ru.slie.luna.sdk.project;

import org.eclipse.aether.artifact.Artifact;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.stream.Stream;

public class DemoGenerator {
    private final static String folderPath = "/templates/demo/";
    private final static String DEMO_GROUP_ID = "ru.slie.luna.template";
    private final static String DEMO_ARTIFACT_ID = "demo-addon";
    private final Path targetDir;
    private final String targetGroupId;
    private final String targetArtifactId;
    private final String targetVersion;

    public DemoGenerator(Path targetDir, Artifact targetArtifact) {
        this.targetDir = targetDir;
        this.targetGroupId = targetArtifact.getGroupId();
        this.targetArtifactId = targetArtifact.getArtifactId();
        this.targetVersion = targetArtifact.getVersion();
    }

    private String getContent(Path source) throws IOException {
        String content = Files.readString(source);
        if (source.toString().endsWith(".java")) {
            content = content.replaceAll(DEMO_GROUP_ID, targetGroupId);
        } else if (source.toString().endsWith(".pom")) {
            content = content.replaceAll(DEMO_GROUP_ID, targetGroupId);
            content = content.replaceAll(DEMO_ARTIFACT_ID, targetArtifactId);
            content = content.replaceAll("<version>1.0.0-SNAPSHOT</version>", "<version>" + targetVersion + "</version>");
        } else if (source.toString().endsWith(".yaml") || source.toString().endsWith(".yml")) {
            content = content.replaceAll(DEMO_GROUP_ID, targetGroupId);
        }

        return content;
    }

    public void processFiles() throws URISyntaxException, IOException {
        URL resource = getClass().getResource(folderPath);
        if (resource == null) {
            return;
        }

        URI uri = resource.toURI();
        try (FileSystem ignore = uri.getScheme().equals("jar")? FileSystems.newFileSystem(uri, Collections.emptyMap()): null) {
            Path dirPath = Paths.get(uri);
            Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
                @Override
                @NonNull
                public FileVisitResult visitFile(@NonNull Path filePath, @NonNull BasicFileAttributes attrs) throws IOException {
                    Path targetPath = translatePath(dirPath, filePath);
                    Files.createDirectories(targetDir.resolve(targetPath.getParent()));
                    Files.writeString(targetPath, getContent(filePath), StandardCharsets.UTF_8);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private Path translatePath(Path templateDir, Path filePath) {
        String translatedPath = templateDir.relativize(filePath).toString().replace('\\', '/');
        if (translatedPath.startsWith("src/main/java")) {
            translatedPath = translatedPath.replace(DEMO_GROUP_ID.replaceAll("\\.", "/"), targetGroupId.replaceAll("\\.", "/"));
        }

        return Path.of(targetDir.toString(), translatedPath);
    }

    private void processFile(Path sourceFile, Path targetDir, Artifact targetArtifact) throws IOException {

//        if (Files.isRegularFile(sourceFile)) {
//            String fileName = sourceFile.getFileName().toString();
//            String content = Files.readString(sourceFile);
//            content = content.replaceAll(DEMO_GROUP_ID, targetArtifact.getGroupId() + "." + targetArtifact.getArtifactId().replaceAll("-", "_"));
//            if (fileName.endsWith(".pom")) {
//                content = content.replaceAll(DEMO_ARTIFACT_ID, targetArtifact.getArtifactId());
//                content = content.replaceAll("<version>1.0.0-SNAPSHOT</version>", "<version>" + targetArtifact.getVersion() + "</version>");
//            }
//            Path targetPath;
//            if (fileName.endsWith(".java")) {
//                String directory = targetDir.toString().replaceAll(DEMO_GROUP_ID.replaceAll("\\.", "/"))
//            } else {
//                targetPath = targetDir.resolve(fileName);
//            }
//
//            Files.createDirectories(targetDir.resolve(targetPath.getParent()));
//            //Path targetPath = targetDir.resolve(fileName);
//            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
//        } else if (Files.isDirectory(sourceFile)) {
//            try (Stream<Path> stream = Files.list(sourceFile)) {
//
//                for (Path file: stream.toList()) {
//                    processFile(file, targetDir.resolve(sourceFile.getFileName()), targetArtifact);
//                }
//            }
//        }
    }

    public void copyDemoFiles(Path targetDir, Artifact artifact) throws URISyntaxException, IOException {
        URL resource = getClass().getResource(folderPath);
        if (resource == null) {
            return;
        }

        Files.createDirectories(targetDir);

        URI uri = resource.toURI();
        try (FileSystem ignore = uri.getScheme().equals("jar")? FileSystems.newFileSystem(uri, Collections.emptyMap()): null) {
            Path dirPath = Paths.get(uri);
            try (Stream<Path> stream = Files.list(dirPath)) {
                for (Path file: stream.toList()) {
                    processFile(file, targetDir, artifact);
                }
            }
        }
    }
}

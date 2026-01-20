package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.slie.luna.plugins.quick_reload.models.PluginFile;
import ru.slie.luna.system.plugin.PluginManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuickReloadInstaller {
    private static final Logger log = LoggerFactory.getLogger(QuickReloadInstaller.class);
    private static final Pattern JAR_PATTERN = Pattern.compile("^(.*?)-(\\d+(?:\\.\\d+)*)(?:-jar-with-dependencies)?\\.jar$");

    private final PluginManager pluginManager;

    public QuickReloadInstaller(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void installPlugin(Path pluginJar) {
        log.info("Plugin changed, {}", pluginJar);
    }


    public void loadLatestPluginsAtStartup(Path targetDirectory) {
        Map<String, List<PluginFile>> pluginsByName = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDirectory, "*.jar")) {
            for (Path path: stream) {
                String fileName = path.getFileName().toString();
                Matcher matcher = JAR_PATTERN.matcher(fileName);
                if (matcher.matches()) {
                    String pluginName = matcher.group(1);
                    String version = matcher.group(2);
                    boolean withDeps = fileName.contains("-jar-with-dependencies");

                    pluginsByName
                            .computeIfAbsent(pluginName, k -> new ArrayList<>())
                            .add(new PluginFile(pluginName, version, withDeps, path));
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan directory {}", targetDirectory, e);
            return;
        }

        for (List<PluginFile> candidates : pluginsByName.values()) {
            if (candidates.isEmpty()) {
                continue;
            }

            PluginFile pluginFile = candidates.stream().max(PluginFile::compare).get();
            log.info("Founded plugin {}: {} (withDeps={})", pluginFile.pluginName(), pluginFile.version(), pluginFile.withDeps());
            installPlugin(pluginFile.path());
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = Integer.parseInt(parts1[i]);
            int p2 = Integer.parseInt(parts2[i]);
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return Integer.compare(parts1.length, parts2.length);
    }
}

package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.slie.luna.locale.I18nResolver;
import ru.slie.luna.plugins.quick_reload.models.PluginFile;
import ru.slie.luna.system.plugin.PluginManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuickReloadInstaller {
    private static final Logger log = LoggerFactory.getLogger(QuickReloadInstaller.class);
    private static final Pattern JAR_PATTERN = Pattern.compile("^(.*?)-(\\d+(?:\\.\\d+)*)(?:-jar-with-dependencies)?\\.jar$");

    private final I18nResolver i18n;
    private final PluginManager pluginManager;

    public QuickReloadInstaller(I18nResolver i18n, PluginManager pluginManager) {
        this.i18n = i18n;
        this.pluginManager = pluginManager;
    }

    public void installPlugin(Path pluginJar) {
        if (!getCandidates(pluginJar.getParent()).contains(pluginJar)) {
            return;
        }

        try (InputStream inputStream = Files.newInputStream(pluginJar)) {
            pluginManager.installPlugin(inputStream);
        } catch (Exception e) {
            log.error(i18n.getText("quickreload.service.failed_read_file", pluginJar.toString()), e);
        }
    }

    public List<Path> getCandidates(Path targetDirectory) {
        Map<String, List<PluginFile>> pluginsByName = new HashMap<>();
        log.info(i18n.getText("quickreload.service.start_scan_dir", targetDirectory.toString()));

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
            log.error(i18n.getText("quickreload.service.failed_scan_dir", targetDirectory.toString()), e);
            return List.of();
        }

        List<Path> out = new ArrayList<>();

        for (List<PluginFile> candidates : pluginsByName.values()) {
            if (candidates.isEmpty()) {
                continue;
            }

            PluginFile pluginFile = candidates.stream().max(PluginFile::compare).get();
            log.info(i18n.getText("quickreload.service.fonded_file", pluginFile.path().toString()));
            out.add(pluginFile.path());
        }

        return out;
    }
}

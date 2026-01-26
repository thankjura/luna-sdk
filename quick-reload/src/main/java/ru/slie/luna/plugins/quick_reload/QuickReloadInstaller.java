package ru.slie.luna.plugins.quick_reload;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.slie.luna.locale.I18nResolver;
import ru.slie.luna.plugins.quick_reload.models.PluginFile;
import ru.slie.luna.system.plugin.PluginDescriptor;
import ru.slie.luna.system.plugin.PluginDescriptorParser;
import ru.slie.luna.system.plugin.PluginManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuickReloadInstaller {
    private static final Logger log = LoggerFactory.getLogger(QuickReloadInstaller.class);
    private static final Pattern JAR_PATTERN = Pattern.compile("^(?<name>.*?)-(?<version>\\d+(?:\\.\\d+.*?)*)(?<deps>-jar-with-dependencies)?\\.jar$");

    private final I18nResolver i18n;
    private final PluginManager pluginManager;
    private final PluginDescriptorParser pluginDescriptorParser;

    public QuickReloadInstaller(I18nResolver i18n,
                                PluginManager pluginManager,
                                PluginDescriptorParser pluginDescriptorParser) {
        this.i18n = i18n;
        this.pluginManager = pluginManager;
        this.pluginDescriptorParser = pluginDescriptorParser;
    }

    public Optional<PluginDescriptor> getDescriptor(Path pluginPath) {
        try {
            return Optional.of(pluginDescriptorParser.getPluginDescriptor(pluginPath));
        } catch (Exception e) {
            log.warn(i18n.getText("quickreload.service.failed_read_file", pluginPath.toString()), e);
        }

        return Optional.empty();
    }

    public void installPlugin(Path pluginJar) {
        try (InputStream inputStream = Files.newInputStream(pluginJar)) {
            pluginManager.installPlugin(inputStream);
        } catch (Exception e) {
            log.error(i18n.getText("quickreload.service.failed_read_file", pluginJar.toString()), e);
        }
    }

    public void installPluginIfValid(Path pluginPath, String pluginKey) {
        Optional<PluginDescriptor> descriptor = getDescriptor(pluginPath);
        if (descriptor.isEmpty() || !pluginKey.equals(descriptor.get().getKey())) {
            return;
        }

        Optional<Path> candidate = getCandidate(pluginPath.getParent());
        if (candidate.isPresent() && candidate.get().equals(pluginPath)) {
            installPlugin(pluginPath);
        }
    }

    public Optional<Path> getCandidate(Path targetDirectory) {
        List<PluginFile> plugins = new ArrayList<>();
        log.info(i18n.getText("quickreload.service.start_scan_dir", targetDirectory.toString()));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDirectory, "*.jar")) {
            for (Path path: stream) {
                String fileName = path.getFileName().toString();
                Matcher matcher = JAR_PATTERN.matcher(fileName);
                if (matcher.matches()) {
                    String pluginName = matcher.group("name");
                    String version = matcher.group("version");
                    String deps = matcher.group("deps");
                    plugins.add(new PluginFile(pluginName, new ComparableVersion(version), deps != null, path));
                }
            }
        } catch (IOException e) {
            log.error(i18n.getText("quickreload.service.failed_scan_dir", targetDirectory.toString()), e);
            return Optional.empty();
        }

        if (plugins.isEmpty()) {
            return Optional.empty();
        }

        Collections.sort(plugins);
        Path pluginPath = plugins.getLast().path();
        log.info(i18n.getText("quickreload.service.fonded_file", pluginPath.toString()));
        return Optional.of(pluginPath);
    }
}

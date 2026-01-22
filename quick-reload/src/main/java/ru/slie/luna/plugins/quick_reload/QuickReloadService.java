package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.slie.luna.annotations.LunaComponent;
import ru.slie.luna.system.plugin.PluginDescriptor;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@LunaComponent
public class QuickReloadService implements InitializingBean, DisposableBean {
    private final Logger log = LoggerFactory.getLogger(QuickReloadService.class);
    private volatile QuickReloadWatcher watcher;
    private final QuickReloadInstaller pluginInstaller;
    private final ResourceMapper resourceMapper;

    public QuickReloadService(QuickReloadInstaller pluginInstaller,
                              ResourceMapper resourceMapper) {
        this.pluginInstaller = pluginInstaller;
        this.resourceMapper = resourceMapper;
    }

    @Override
    public void destroy() throws Exception {
        if (watcher != null) {
            watcher.close();
        }
    }

    public Optional<QuickReloadWatcher> getWatcher() {
        return Optional.ofNullable(watcher);
    }

    @Override
    public void afterPropertiesSet() {
        String pluginDir = System.getProperty("quickreload.dir");
        if (pluginDir == null || pluginDir.trim().isEmpty()) {
            return;
        }

        Path targetPath = Path.of(pluginDir);
        if (!Files.isDirectory(targetPath)) {
            return;
        }

        Path pluginDescriptorPath = targetPath.resolve("classes").resolve("luna-plugin.yaml");
        if (!Files.isRegularFile(pluginDescriptorPath)) {
            return;
        }
        YAMLMapper mapper = new YAMLMapper();
        PluginDescriptor descriptor;
        try {
             descriptor = mapper.readValue(pluginDescriptorPath, PluginDescriptor.class);
        } catch (Exception e) {
            return;
        }

        if (descriptor == null) {
            return;
        }

        Optional<Path> candidate = pluginInstaller.getCandidate(targetPath);
        if (candidate.isPresent()) {
            Optional<PluginDescriptor> candidateDescriptor = pluginInstaller.getDescriptor(candidate.get());
            if (candidateDescriptor.isPresent() && descriptor.getKey().equals(candidateDescriptor.get().getKey())) {
                pluginInstaller.installPlugin(candidate.get());
            }
        }

        resourceMapper.addReplace("/rest/resources/" + descriptor.getKey(), targetPath.getParent().resolve("src").resolve("main").resolve("resources"));

        log.info("Start watching plugin dir {}", targetPath);

        try {
            watcher = new QuickReloadWatcher(targetPath, (Path pluginPath) -> {
                pluginInstaller.installPluginIfValid(pluginPath, descriptor.getKey());
            });
        } catch (IOException e) {
            log.error("Failed to start watch directory {}", targetPath, e);
        }
    }
}

package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.slie.luna.annotations.LunaComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@LunaComponent
public class PluginReloadService implements InitializingBean, DisposableBean {
    private final Logger log = LoggerFactory.getLogger(PluginReloadService.class);
    private volatile PluginWatcher watcher;
    private final PluginInstaller pluginInstaller;

    public PluginReloadService(PluginInstaller pluginInstaller) {
        this.pluginInstaller = pluginInstaller;
    }

    @Override
    public void destroy() throws Exception {
        if (watcher != null) {
            watcher.close();
        }
    }

    @Override
    public void afterPropertiesSet() {
        String pluginDir = System.getProperty("quickrealod.dir");
        if (pluginDir == null || pluginDir.trim().isEmpty()) {
            return;
        }

        Path targetPath = Path.of(pluginDir);
        if (!Files.isDirectory(targetPath)) {
            return;
        }

        log.info("Start watching plugin dir {}", targetPath);

        try {
            watcher = new PluginWatcher(targetPath, pluginInstaller::installPlugin);
        } catch (IOException e) {
            log.error("Failed to start watch directory {}", targetPath, e);
        }
    }
}

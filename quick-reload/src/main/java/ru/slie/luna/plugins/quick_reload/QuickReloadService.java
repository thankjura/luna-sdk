package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.slie.luna.annotations.LunaComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@LunaComponent
public class QuickReloadService implements InitializingBean, DisposableBean {
    private final Logger log = LoggerFactory.getLogger(QuickReloadService.class);
    private volatile QuickReloadWatcher watcher;
    private final QuickReloadInstaller pluginInstaller;

    public QuickReloadService(QuickReloadInstaller pluginInstaller) {
        this.pluginInstaller = pluginInstaller;
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

        log.info("Start watching plugin dir {}", targetPath);

        try {
            watcher = new QuickReloadWatcher(targetPath, pluginInstaller::installPlugin);
        } catch (IOException e) {
            log.error("Failed to start watch directory {}", targetPath, e);
        }
    }
}

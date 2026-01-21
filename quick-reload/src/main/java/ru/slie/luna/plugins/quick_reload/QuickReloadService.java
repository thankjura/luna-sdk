package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.slie.luna.annotations.LunaComponent;
import ru.slie.luna.locale.I18nResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@LunaComponent
public class QuickReloadService implements InitializingBean, DisposableBean {
    private final Logger log = LoggerFactory.getLogger(QuickReloadService.class);
    private volatile QuickReloadWatcher watcher;
    private final QuickReloadInstaller pluginInstaller;
    private final I18nResolver i18n;

    public QuickReloadService(QuickReloadInstaller pluginInstaller,
                              I18nResolver i18n) {
        this.pluginInstaller = pluginInstaller;
        this.i18n = i18n;
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

        List<Path> candidates = pluginInstaller.getCandidates(targetPath);
        for (Path file: candidates) {
            pluginInstaller.installPlugin(file);
        }

        log.info("Start watching plugin dir {}", targetPath);

        try {
            watcher = new QuickReloadWatcher(targetPath, pluginInstaller::installPlugin);
        } catch (IOException e) {
            log.error("Failed to start watch directory {}", targetPath, e);
        }
    }
}

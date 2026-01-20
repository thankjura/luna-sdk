package ru.slie.luna.plugins.quick_reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.slie.luna.system.plugin.PluginManager;

import java.nio.file.Path;

@Component
public class QuickReloadInstaller {
    private static final Logger log = LoggerFactory.getLogger(QuickReloadInstaller.class);
    private final PluginManager pluginManager;

    public QuickReloadInstaller(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void installPlugin(Path pluginJar) {
        log.info("Plugin changed, {}", pluginJar);
    }
}

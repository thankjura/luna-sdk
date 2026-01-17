package ru.slie.luna.plugins.plugin_watcher;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.slie.luna.annotations.LunaComponent;

@LunaComponent
public class PluginReloadService implements InitializingBean, DisposableBean {

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}

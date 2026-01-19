package ru.slie.luna.plugins.quick_reload;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.slie.luna.annotations.LunaComponent;

@LunaComponent
public class PluginReloadService implements InitializingBean, DisposableBean {

    @Override
    public void destroy() throws Exception {
        System.out.println("Пока!");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("Я тут");
    }
}

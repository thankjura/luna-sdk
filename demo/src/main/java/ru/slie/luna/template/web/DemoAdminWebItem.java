package ru.slie.luna.template.web;

import org.springframework.stereotype.Component;
import ru.slie.luna.locale.I18nResolver;
import ru.slie.luna.web.WebItem;

import java.util.Map;

@Component
public class DemoAdminWebItem implements WebItem {
    private final I18nResolver i18n;

    public DemoAdminWebItem(I18nResolver i18n) {
        this.i18n = i18n;
    }

    @Override
    public String getId() {
        return "console";
    }

    @Override
    public String getLocation() {
        return "admin/plugins/demo";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return i18n.getText("demo.web.item.label");
    }

    @Override
    public String getRouteName() {
        return "myDemoPage";
    }

    @Override
    public Map<String, String> getRouteParams() {
        return Map.of();
    }
}

package ru.slie.luna.template.web;

import org.springframework.stereotype.Component;
import ru.slie.luna.locale.I18nResolver;
import ru.slie.luna.web.WebItem;

import java.util.Map;

@Component
public class TodoAdminWebItem implements WebItem {
    private final I18nResolver i18n;

    public TodoAdminWebItem(I18nResolver i18n) {
        this.i18n = i18n;
    }

    @Override
    public String getId() {
        return "todoItem";
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
        // routeName from luna-plugin.yaml
        return "myTodoPage";
    }

    @Override
    public Map<String, String> getRouteParams() {
        return Map.of();
    }
}

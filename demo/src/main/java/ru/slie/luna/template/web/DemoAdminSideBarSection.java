package ru.slie.luna.template.web;

import org.springframework.stereotype.Component;
import ru.slie.luna.locale.I18nResolver;
import ru.slie.luna.web.WebSection;

@Component
public class DemoAdminSideBarSection implements WebSection {
    private final I18nResolver i18n;

    public DemoAdminSideBarSection(I18nResolver i18n) {
        this.i18n = i18n;
    }

    @Override
    public String getId() {
        return "demo";
    }

    @Override
    public String getLocation() {
        return "admin/plugins";
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public String getName() {
        return i18n.getText("demo.web.section.label");
    }
}

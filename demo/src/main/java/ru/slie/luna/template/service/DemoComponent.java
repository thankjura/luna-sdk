package ru.slie.luna.template.service;

import org.springframework.stereotype.Component;
import ru.slie.luna.locale.I18nResolver;

@Component
public class DemoComponent {
    private final I18nResolver i18n;

    public DemoComponent(I18nResolver i18n) {
        this.i18n = i18n;
    }

    public String getMessage() {
        return i18n.getText("demo.hello");
    }
}

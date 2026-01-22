package ru.slie.luna.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.slie.luna.sdk.utils.I18nResolver;

@RestController
@RequestMapping("/rest/my-addon")
public class HelloWorldRest {
    private final I18nResolver i18n;

    public HelloWorldRest(I18nResolver i18n) {
        this.i18n = i18n;
    }

    @GetMapping({"/", ""})
    public String hello() {
        return i18n.getString("app.hello");
    }
}

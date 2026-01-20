package ru.slie.luna.plugins.quick_reload.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.slie.luna.locale.I18nResolver;
import ru.slie.luna.plugins.quick_reload.QuickReloadService;
import ru.slie.luna.plugins.quick_reload.QuickReloadWatcher;
import ru.slie.luna.plugins.quick_reload.rest.response.QuickReloadState;

import java.util.Optional;

@RestController
@RequestMapping("/rest/quickreload")
public class MonitorRest {
    private final QuickReloadService quickReloadService;
    private final I18nResolver i18n;

    public MonitorRest(QuickReloadService quickReloadService,
                       I18nResolver i18n) {
        this.quickReloadService = quickReloadService;
        this.i18n = i18n;
    }

    @GetMapping({"", "/"})
    public QuickReloadState state() {
        Optional<QuickReloadWatcher> watcher = quickReloadService.getWatcher();
        String message = i18n.getText("quickreload.hello");
        return watcher.map(quickReloadWatcher -> new QuickReloadState(quickReloadWatcher.getWatchDirectory(), message)).orElseGet(() -> new QuickReloadState(null, message));
    }
}

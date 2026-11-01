package ru.slie.luna.plugins.quick_reload.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.slie.luna.plugins.quick_reload.QuickReloadService;
import ru.slie.luna.plugins.quick_reload.QuickReloadWatcher;
import ru.slie.luna.plugins.quick_reload.rest.response.QuickReloadState;

import java.util.Optional;

@RestController
@RequestMapping("/rest/quickreload")
public class MonitorRest {
    private final QuickReloadService quickReloadService;

    public MonitorRest(QuickReloadService quickReloadService) {
        this.quickReloadService = quickReloadService;
    }

    @GetMapping({"", "/"})
    public QuickReloadState state() {
        Optional<QuickReloadWatcher> watcher = quickReloadService.getWatcher();
        return watcher.map(quickReloadWatcher -> new QuickReloadState(quickReloadWatcher.getWatchDirectory())).orElseGet(() -> new QuickReloadState(null));
    }
}

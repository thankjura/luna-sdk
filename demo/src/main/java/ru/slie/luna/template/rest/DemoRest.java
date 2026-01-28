package ru.slie.luna.template.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.slie.luna.template.service.DemoComponent;

@RestController
@RequestMapping("/demo")
public class DemoRest {
    private final DemoComponent demoComponent;

    public DemoRest(DemoComponent demoComponent) {
        this.demoComponent = demoComponent;
    }

    @GetMapping({"", "/"})
    public DemoResponse getHello() {
        return new DemoResponse(demoComponent.getMessage());
    }

    public record DemoResponse (String message) {}
}

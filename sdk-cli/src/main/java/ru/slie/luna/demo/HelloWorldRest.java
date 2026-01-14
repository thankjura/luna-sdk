package ru.slie.luna.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/my-addon")
public class HelloWorldRest {
    @GetMapping({"/", ""})
    public String hello() {
        return "hello from rest";
    }
}

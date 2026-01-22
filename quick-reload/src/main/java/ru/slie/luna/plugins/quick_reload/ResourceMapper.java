package ru.slie.luna.plugins.quick_reload;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

@Component
public class ResourceMapper {
    private final Map<String, Path> replaces;

    public ResourceMapper(Map<String, Path> replaces) {
        this.replaces = replaces;
    }

    public void addReplace(String uri, Path fileLocation) {
        replaces.put(uri, fileLocation);
    }

    public Map<String, Path> getReplaces() {
        return replaces;
    }
}

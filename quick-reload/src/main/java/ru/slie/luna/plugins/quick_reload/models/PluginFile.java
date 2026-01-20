package ru.slie.luna.plugins.quick_reload.models;

import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

public record PluginFile(String pluginName, String version, boolean withDeps, Path path) implements Comparable<PluginFile> {

    public static int compare(PluginFile a, PluginFile b) {
        if (a.version.equals(b.version)) {
            if (a.withDeps) {
                return 1;
            } else if (b.withDeps) {
                return -1;
            } else {
                return 0;
            }
        }
        String[] parts1 = a.version.split("\\.");
        String[] parts2 = b.version.split("\\.");
        int length = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = Integer.parseInt(parts1[i]);
            int p2 = Integer.parseInt(parts2[i]);
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return Integer.compare(parts1.length, parts2.length);
    }

    @Override
    public int compareTo(@NonNull PluginFile pluginFile) {
        return compare(this, pluginFile);
    }
}

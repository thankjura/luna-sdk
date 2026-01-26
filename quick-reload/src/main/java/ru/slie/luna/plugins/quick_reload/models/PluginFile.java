package ru.slie.luna.plugins.quick_reload.models;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

public record PluginFile(String pluginName, ComparableVersion version, boolean withDeps, Path path) implements Comparable<PluginFile> {
    public static int compare(PluginFile a, PluginFile b) {
        int out = a.version.compareTo(b.version);
        if (out == 0) {
            if (a.withDeps) {
                return 1;
            } else if (b.withDeps) {
                return -1;
            }
        }

        return out;
    }

    @Override
    public int compareTo(@NonNull PluginFile pluginFile) {
        return compare(this, pluginFile);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("PluginFile {f: %s,v: %s, d: %s}", pluginName, version, withDeps);
    }
}

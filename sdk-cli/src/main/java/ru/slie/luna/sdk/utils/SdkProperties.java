package ru.slie.luna.sdk.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Build time SDK properties (luna-sdk.properties, filtered by Maven).
 */
public final class SdkProperties {
    private static final String RESOURCE = "luna-sdk.properties";
    private static final Properties PROPERTIES = load();

    private SdkProperties() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream is = SdkProperties.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException(key + " is not defined in " + RESOURCE);
        }
        return value;
    }
}

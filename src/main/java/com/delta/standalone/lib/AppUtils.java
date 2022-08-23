package com.delta.standalone.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppUtils {
    public static Properties loadProperties() throws IOException {
        Properties configuration = new Properties();
        InputStream inputStream = AppUtils.class
                .getClassLoader()
                .getResourceAsStream("application.properties");
        configuration.load(inputStream);
        if (inputStream != null) {
            inputStream.close();
        }
        return configuration;
    }
}

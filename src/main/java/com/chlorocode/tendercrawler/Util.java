package com.chlorocode.tendercrawler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This is a utility class to share common functions used by the application.
 */
public class Util {

    /**
     * This method is used to get configuration value from properties file.
     *
     * @param key configuration key
     * @return configuration value
     * @throws IOException if error when reading properties file
     */
    public static String getConfigValue(String key) throws IOException {
        Properties prop = new Properties();
        InputStream input;
        input = Util.class.getClassLoader().getResourceAsStream("config.properties");
        prop.load(input);

        return prop.getProperty(key);
    }
}

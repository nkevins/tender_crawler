package com.chlorocode.tendercrawler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Util {

    public static String getConfigValue(String key) throws IOException {
        Properties prop = new Properties();
        InputStream input;
        input = Util.class.getClassLoader().getResourceAsStream("config.properties");
        prop.load(input);

        return prop.getProperty(key);
    }
}

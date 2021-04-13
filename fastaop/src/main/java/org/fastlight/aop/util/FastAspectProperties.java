package org.fastlight.aop.util;

import java.util.Properties;

/**
 * @author ychost
 * @date 2021-04-13
 **/
public class FastAspectProperties {
    /**
     * 配置文件
     */
    private static final String PROPERTIES_FILE = "META-INF/aspect/aspect.config.properties";
    /**
     * 加载的配置结果
     */
    private static final Properties PROPERTIES;

    /**
     * 初始化配置
     */
    static {
        PROPERTIES = new Properties();
        try {
            PROPERTIES.load(FastAspectProperties.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取配置属性
     */
    public static String getProperty(String name) {
        return PROPERTIES.getProperty(name);
    }

}

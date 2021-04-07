package org.fastlight.aop.processor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.fastlight.aop.annotation.FastAspect;

/**
 * 用户可以自由添加注解到 supports.txt 里面实现切面注入
 *
 * @author ychost
 * @date 2021-04-07
 **/
public class AspectSupportTypes {
    private static final String TYPES_FILE = "META-INF/aspect/fast.aspect.supports.txt";

    private static Set<String> SUPPORT_TYPES = null;

    private static final Object SUPPORT_LOCKER = new Object();

    public static Set<String> getSupportTypes() {
        if (SUPPORT_TYPES != null) {
            return SUPPORT_TYPES;
        }
        try {
            synchronized (SUPPORT_LOCKER) {
                if (SUPPORT_TYPES != null) {
                    return SUPPORT_TYPES;
                }
                SUPPORT_TYPES = Sets.newHashSet();
                // 加入默认植入注解
                SUPPORT_TYPES.add(FastAspect.class.getName());
                // 扫描用户自定义注解
                Enumeration<URL> urls = AspectSupportTypes.class.getClassLoader().getResources(TYPES_FILE);
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), Charsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String aspect = line.split("#")[0].trim();
                            if (StringUtils.isNotBlank(aspect)) {
                                SUPPORT_TYPES.add(aspect);
                            }
                        }
                    }
                }
                return SUPPORT_TYPES;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

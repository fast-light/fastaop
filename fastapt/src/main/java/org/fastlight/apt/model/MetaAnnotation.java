package org.fastlight.apt.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.fastlight.core.util.ReflectUtils;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

/**
 * 注解元数据
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@SuppressWarnings("unchecked")
public class MetaAnnotation {
    /**
     * 注解类型
     */
    public Class<? extends Annotation> type;

    /**
     * 注解值，含 default
     */
    public Map<String, Object> args;

    /**
     * 构造一个 MetaAnnotation
     *
     * @param type 注解类型
     * @param args 注解值，含 default，对于 "xxx.class" 的会直接转换成 class
     * @return 注解元数据
     */
    public static MetaAnnotation create(Class<? extends Annotation> type, Map<String, Object> args) {
        MetaAnnotation annotation = new MetaAnnotation();
        annotation.type = type;
        Set<String> keys = Sets.newHashSet(args.keySet());
        Map<String, Object> atArgs = Maps.newHashMap();
        for (String key : keys) {
            Object data = args.get(key);
            if (data instanceof String && data.toString().endsWith(".class")) {
                data = ReflectUtils.forNameCache(data.toString().replaceAll("\\.class", ""));
            }
            atArgs.put(key, data);
        }
        annotation.args = atArgs;
        return annotation;
    }

    /**
     * 获取注解的某个属性的值
     *
     * @param prop 属性
     * @param <T>  属性值的类型
     * @return 属性的值
     */
    public <T> T getValue(String prop) {
        if (args == null) {
            return null;
        }
        return (T) args.get(prop);
    }

    public Class<? extends Annotation> getType() {
        return type;
    }

    public Map<String, Object> getArgs() {
        return args;
    }
}

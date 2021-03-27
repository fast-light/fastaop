package org.fastlight.apt.model;

import java.lang.annotation.Annotation;
import java.util.Map;

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

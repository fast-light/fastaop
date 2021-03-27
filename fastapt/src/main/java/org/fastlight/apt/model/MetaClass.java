package org.fastlight.apt.model;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class MetaClass {
    /**
     * class 的类型
     */
    private Class<?> type;

    /**
     * 类的注解元素
     */
    private MetaAnnotation[] annotations;

    public Class<?> getType() {
        return type;
    }

    public MetaAnnotation[] getAnnotations() {
        return annotations;
    }
}

package org.fastlight.apt.model;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class MetaType {
    /**
     * class 的类型
     */
    private Class<?> type;

    /**
     * 类的注解元素
     */
    private MetaAnnotation[] annotations;

    /**
     * 构造一个类的元数据
     */
    public static MetaType create(
        Class<?> type,
        MetaAnnotation[] annotations
    ) {
        MetaType metaType = new MetaType();
        metaType.type = type;
        metaType.annotations = annotations;
        return metaType;
    }

    public Class<?> getType() {
        return type;
    }

    public MetaAnnotation[] getAnnotations() {
        return annotations;
    }
}

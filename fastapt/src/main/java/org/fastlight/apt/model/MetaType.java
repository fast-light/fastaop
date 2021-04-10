package org.fastlight.apt.model;

import java.lang.annotation.Annotation;
import java.util.Arrays;

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

    /**
     * 是否类上面包含某个注解
     */
    public boolean isAnnotated(Class<? extends Annotation> cls) {
        return Arrays.stream(annotations).anyMatch(v -> cls.equals(v.getType()));
    }
}

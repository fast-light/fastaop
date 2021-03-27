package org.fastlight.apt.model;

import org.fastlight.core.util.ReflectUtils;

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

    /**
     * 构造一个类的元数据
     */
    public static MetaClass create(
            Object ownerType,
            MetaAnnotation[] annotations
    ) {
        MetaClass metaClass = new MetaClass();
        if (ownerType instanceof Class) {
            metaClass.type = (Class<?>) ownerType;
        } else {
            metaClass.type = ReflectUtils.forNameCache(ownerType.toString());
        }
        metaClass.annotations = annotations;
        return metaClass;
    }

    public Class<?> getType() {
        return type;
    }

    public MetaAnnotation[] getAnnotations() {
        return annotations;
    }
}

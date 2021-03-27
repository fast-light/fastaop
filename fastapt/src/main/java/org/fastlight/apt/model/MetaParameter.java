package org.fastlight.apt.model;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class MetaParameter {
    /**
     * 参数类型
     */
    private Class<?> type;

    /**
     * 参数名
     */
    private String name;

    /**
     * 参数上面的注解信息
     */
    private MetaAnnotation[] annotations;

    /**
     * 构造一个参数元数据
     *
     * @param type        参数类型
     * @param name        参数名字
     * @param annotations 参数上面的注解
     * @return 参数元数据
     */
    public static MetaParameter create(Class<?> type, String name, MetaAnnotation[] annotations) {
        MetaParameter parameter = new MetaParameter();
        parameter.type = type;
        parameter.name = name;
        parameter.annotations = annotations;
        return parameter;
    }

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}

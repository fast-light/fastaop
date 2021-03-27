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

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}

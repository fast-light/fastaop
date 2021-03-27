package org.fastlight.apt.model.compile;

import com.sun.tools.javac.code.Type;

import java.util.List;

/**
 * 保存编译过程中的参数信息
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class ParameterCompile {
    /**
     * 参数名
     */
    private String name;

    /**
     * 参数类型
     */
    private Type type;

    /**
     * 参数注解
     */
    private List<AnnotationCompile> annotations;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<AnnotationCompile> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationCompile> annotations) {
        this.annotations = annotations;
    }
}

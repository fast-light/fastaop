package org.fastlight.apt.model.compile;

import com.sun.tools.javac.code.Type;

import java.util.List;

/**
 * 保存编译过程中的注解信息
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class AnnotationCompile {
    /**
     * 注解数据
     */
    List<AnnotationInfo> infos;

    /**
     * 注解类型
     */
    private Type type;

    public List<AnnotationInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<AnnotationInfo> infos) {
        this.infos = infos;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public static class AnnotationInfo {
        /**
         * 属性名
         */
        private String name;

        /**
         * 属性值
         */
        private Object value;

        /**
         * 注解值类型
         */
        private Type valueType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Type getValueType() {
            return valueType;
        }

        public void setValueType(Type valueType) {
            this.valueType = valueType;
        }
    }
}

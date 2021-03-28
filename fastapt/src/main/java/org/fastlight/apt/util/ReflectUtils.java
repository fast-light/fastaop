package org.fastlight.apt.util;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * @author ychost
 * @date 2020-03-20
 **/
public class ReflectUtils {
    /**
     * 原生类型解析，比如 V,Z,B 这些都是字节码的东西
     */
    public static final Map<String, Class<?>> PRIMITIVE_CLASS;

    public static final Map<String, Class<?>> CLASS_CACHE = Maps.newConcurrentMap();

    /**
     * int/double 等映射 I/D 字节码东西
     */
    public static final Map<String, String> PRIMITIVE_CLASS_NAME;

    static {
        Map<String, Class<?>> primitiveClass = Maps.newHashMap();
        primitiveClass.put("V", Void.TYPE);
        primitiveClass.put("Z", Boolean.TYPE);
        primitiveClass.put("B", Byte.TYPE);
        primitiveClass.put("C", Character.TYPE);
        primitiveClass.put("S", Short.TYPE);
        primitiveClass.put("I", Integer.TYPE);
        primitiveClass.put("L", Long.TYPE);
        primitiveClass.put("F", Float.TYPE);
        primitiveClass.put("D", Double.TYPE);
        PRIMITIVE_CLASS = Collections.unmodifiableMap(primitiveClass);
    }

    static {
        Map<String, String> map = Maps.newHashMap();
        map.put("void", "V");
        map.put("boolean", "Z");
        map.put("byte", "B");
        map.put("char", "C");
        map.put("short", "S");
        map.put("int", "I");
        map.put("long", "L");
        map.put("float", "F");
        map.put("double", "D");
        PRIMITIVE_CLASS_NAME = map;
    }

    /**
     * 获取 Class<A,B,C> 里面的 A,B,C
     */
    public static Type[] getGenericTypes(Class<?> cls) {
        Type type = cls.getGenericSuperclass();
        ParameterizedType pt = (ParameterizedType) type;
        return pt.getActualTypeArguments();
    }

    /**
     * 代理 {@link Class#forName(String, boolean, ClassLoader)}，支持原生类型
     *
     * @param clsName
     * @param initialize
     * @param classLoader
     * @return
     */
    public static Class<?> forName(String clsName, boolean initialize, ClassLoader classLoader)
            throws ClassNotFoundException {
        clsName = resolveClassName(clsName);
        Class<?> primitiveType = PRIMITIVE_CLASS.get(clsName);
        if (primitiveType != null) {
            return primitiveType;
        }
        return Class.forName(clsName, initialize, classLoader);
    }

    /**
     * 兼容编译过程中的类名
     */
    public static String resolveClassName(String className) {
        String name = PRIMITIVE_CLASS_NAME.get(className);
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        if (PRIMITIVE_CLASS.containsKey(className)) {
            return className;
        }
        // 去除泛型，比如 Map<> -> Map
        int grStart = StringUtils.indexOf(className, "<");
        if (grStart > 0) {
            className = className.substring(0, grStart);
        }
        // 数组处理比较特殊
        // java.lang.String[] -> [Ljava.lang.String;
        // int[] -> [I;
        if (className.endsWith("[]")) {
            int dim = StringUtils.countMatches(className, "[");
            String prefix = StringUtils.repeat("[", dim);
            String midName = className.replaceAll("\\[\\]", "");
            if (PRIMITIVE_CLASS_NAME.containsKey(midName)) {
                midName = PRIMITIVE_CLASS_NAME.get(midName);
                return prefix + midName;
            }
            return prefix + "L" + midName + ";";
        }
        return className;
    }

    /**
     * 带缓存，可提升性能，注意用的默认的 classLoader
     */
    public static Class<?> forNameCache(String clsName) {
        try {
            Class<?> cls = CLASS_CACHE.get(clsName);
            if (cls != null) {
                return cls;
            }
            cls = forName(clsName, true, ReflectUtils.class.getClassLoader());
            CLASS_CACHE.put(clsName, cls);
            return cls;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 给 obj 里面的某种类型的字段赋默认值
     *
     * @param recursive 是否递归给 obj 的对象属性遍历复制，只能当 filedType 是 primaryType 才支持！！
     *                  因为不加这个限制极有可能死循环
     */
    public static <T> void assignDefault(
            Object obj,
            Class<T> assignType,
            T assignValue,
            boolean recursive) throws IllegalAccessException {
        if (obj == null || (!ClassUtils.isPrimitiveOrWrapper(assignType) && recursive)) {
            return;
        }
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            Class<?> type = field.getType();
            field.setAccessible(true);
            Object val = field.get(obj);
            if (type == assignType && val == null) {
                field.set(obj, assignValue);
            } else if (recursive && !ClassUtils.isPrimitiveOrWrapper(type)) {
                assignDefault(val, assignType, assignValue, recursive);
            }
        }
    }

    /**
     * new 一个数组类，支持多维数组，比如 int[][][].class
     *
     * @param cls    int[][][].class
     * @param length 每一维的长度
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")

    public static <T> T newArrayInstance(Class<T> cls, int length) {
        if (!cls.isArray()) {
            return null;
        }
        int cnt = 1;
        Class<?> cmptType = cls.getComponentType();
        while (!cmptType.isArray()) {
            cmptType = cmptType.getComponentType();
            cnt += 1;
        }
        int[] dimensions = new int[cnt];
        Arrays.fill(dimensions, length);
        return (T) Array.newInstance(cmptType, dimensions);
    }
}

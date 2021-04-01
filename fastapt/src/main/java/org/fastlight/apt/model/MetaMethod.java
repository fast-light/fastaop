package org.fastlight.apt.model;

import com.google.common.collect.Maps;
import org.fastlight.apt.annotation.FastMarkedMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

/**
 * 方法元数据
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class MetaMethod {
    /**
     * 该元素在静态 __fast_meta_method 的缓存索引
     */
    private int cacheIndex;

    /**
     * 是否为静态方法
     */
    private boolean isStatic;

    /**
     * 方法名
     */
    private String name;

    /**
     * 方法返回类型
     */
    private Class<?> returnType;

    /**
     * 方法所在类的元数据
     */
    private MetaType metaOwner;

    /**
     * 方法参数
     */
    private MetaParameter[] parameters;

    /**
     * 注解
     */
    private MetaAnnotation[] annotations;

    /**
     * 返回获取当前方法对象，懒汉模式
     */
    private transient Method method;

    /**
     * 构造一个方法元数据
     */
    public static MetaMethod create(Integer cacheIndex, MetaType metaOwner, MetaParameter[] parameters,
        MetaAnnotation[] annotations, Map<String, Object> metaExtension) {
        MetaMethod metaMethod = new MetaMethod();
        metaMethod.cacheIndex = cacheIndex;
        metaMethod.metaOwner = metaOwner;
        metaMethod.parameters = parameters;
        metaMethod.annotations = annotations;
        if (metaExtension != null && metaExtension.size() > 0) {
            metaMethod.metaExtensions.putAll(metaExtension);
        }
        // 赋值 returnType，parameter.type，isStatic
        metaMethod.patchedByReflectMethod();
        return metaMethod;
    }

    /**
     * 全局元数据缓存
     */
    private final Map<String, Object> metaExtensions = Maps.newHashMap();

    public int getCacheIndex() {
        return cacheIndex;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getName() {
        return name;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public MetaType getMetaOwner() {
        return metaOwner;
    }

    public MetaParameter[] getParameters() {
        return parameters;
    }

    public MetaAnnotation[] getAnnotations() {
        return annotations;
    }

    /**
     * @return 当前 Method
     */
    public Method getMethod() {
        if (method != null) {
            return method;
        }
        //https://stackoverflow.com/questions/48113697/getdeclaredmethods-in-class-class
        // isBridge() 是为了防止泛型 Override，JVM 生成多个 method 的情况
        method = Arrays.stream(metaOwner.getType().getDeclaredMethods())
            .filter(v -> !v.isBridge())
            .filter(
                v -> Arrays.stream(v.getAnnotations()).anyMatch(m -> m.annotationType().equals(FastMarkedMethod.class)))
            .filter(v -> v.getAnnotation(FastMarkedMethod.class).value() == cacheIndex)
            .limit(1)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("[FastAop] %s.%s mark index %s match failed", metaOwner.getType().getName(), name,
                    cacheIndex)
            ));
        return method;
    }

    /**
     * 通过反射获取方法将 paramType 和 returnType 进行打补丁 因为这些TYPE 是 T[][][] 这种多维泛型数组的时候，在语法树处理上面不太好搞，这里直接捕获运行时状态，让其更加的准确
     */
    protected void patchedByReflectMethod() {
        Method method = getMethod();
        for (int i = 0; i < method.getParameters().length; i++) {
            parameters[i].setType(method.getParameters()[i].getType());
        }
        returnType = method.getReturnType();
        isStatic = Modifier.isStatic(method.getModifiers());
        name = method.getName();
    }

    public void addMetaExtension(String key, Object value) {
        metaExtensions.put(key, value);
    }

    public <T> T getMetaExtension(String key) {
        // noinspection unchecked
        return (T)metaExtensions.get(key);
    }
}

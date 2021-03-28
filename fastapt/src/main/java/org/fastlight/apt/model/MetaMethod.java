package org.fastlight.apt.model;

import com.google.common.collect.Maps;
import org.fastlight.apt.annotation.FastMarkedMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;

/**
 * 方法元数据
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class MetaMethod {
    /**
     * 该元素在静态 meta_cache 的缓存索引
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
    private MetaClass metaOwner;

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
    public static MetaMethod create(Integer cacheIndex, String name, MetaClass metaOwner, MetaParameter[] parameters,
        MetaAnnotation[] annotations, Map<String, Object> metaExtension) {
        MetaMethod metaMethod = new MetaMethod();
        metaMethod.cacheIndex = cacheIndex;
        metaMethod.metaOwner = metaOwner;
        metaMethod.name = name;
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

    public MetaClass getMetaOwner() {
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
        for (Method declaredMethod : metaOwner.getType().getDeclaredMethods()) {
            for (Annotation annotation : declaredMethod.getAnnotations()) {
                if (!(annotation instanceof FastMarkedMethod)) {
                    continue;
                }
                FastMarkedMethod fastMarkedMethod = (FastMarkedMethod)annotation;
                if (Objects.equals(cacheIndex, fastMarkedMethod.value())) {
                    method = declaredMethod;
                    break;
                }
            }
        }
        if (method != null) {
            if (!name.equals(method.getName())) {
                throw new RuntimeException(String.format("[FastAop] %s.%s is not match marked method %s.%s",
                    metaOwner.getType(), name, metaOwner.getType(), method.getName()));
            }
            return method;
        }
        throw new RuntimeException(String.format("[FastAop] %s.%s not found", metaOwner.getType(), name));
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
    }

    public void addMetaExtension(String key, Object value) {
        metaExtensions.put(key, value);
    }

    public <T> T getMetaExtension(String key) {
        // noinspection unchecked
        return (T)metaExtensions.get(key);
    }
}

package org.fastlight.apt.model;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.fastlight.core.util.ReflectUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public static MetaMethod create(
            Integer cacheIndex,
            boolean isStatic,
            String name,
            MetaClass metaOwner,
            MetaParameter[] parameters,
            Object returnType,
            MetaAnnotation[] annotations,
            Map<String, Object> metaExtension
    ) {
        MetaMethod metaMethod = new MetaMethod();
        metaMethod.cacheIndex = cacheIndex;
        metaMethod.metaOwner = metaOwner;
        metaMethod.isStatic = isStatic;
        metaMethod.name = name;
        metaMethod.parameters = parameters;
        if (returnType instanceof Class) {
            metaMethod.returnType = (Class<?>) returnType;
        } else {
            metaMethod.returnType = ReflectUtils.forNameCache(returnType.toString());
        }
        metaMethod.annotations = annotations;
        if (metaExtension != null && metaExtension.size() > 0) {
            metaMethod.metaExtensions.putAll(metaExtension);
        }
        return metaMethod;
    }

    /**
     * 全局元数据缓存
     */
    private Map<String, Object> metaExtensions = Maps.newHashMap();

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
     * 反射获取 Method 信息
     *
     * @return 当前 Method
     */
    public Method getMethod() {
        if (method == null) {
            method = MethodUtils.getMatchingMethod(
                    metaOwner.getType(),
                    name,
                    Arrays.stream(parameters).map(MetaParameter::getType).toArray(Class<?>[]::new)
            );
        }
        // 有泛型的情况，可能会出现匹配不到，这里只要方法名，方法参数个数，[方法参数名] 相等的唯一匹配即可
        // 因为有泛型，所以就不匹配类型了
        if (method == null) {
            List<Method> methodList = Arrays.stream(metaOwner.getType().getDeclaredMethods())
                    .filter(v -> name.equals(v.getName()))
                    .filter(v -> v.getParameterCount() == parameters.length)
                    .filter(v -> {
                        for (int i = 0; i < v.getParameters().length; i++) {
                            if (v.getParameters()[i].isNamePresent() && !Objects.equals(parameters[i].getName(), v.getParameters()[i].getName())) {
                                return false;
                            }
                        }
                        return true;
                    }).collect(Collectors.toList());
            if (methodList.size() == 1) {
                method = methodList.get(0);
            }
            return method;
        }

        return method;
    }

    public void addMetaExtension(String key, Object value) {
        metaExtensions.put(key, value);
    }

    public <T> T getMetaExtension(String key) {
        //noinspection unchecked
        return (T) metaExtensions.get(key);
    }
}

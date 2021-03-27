package org.fastlight.apt.model;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.fastlight.apt.handler.FastAspectHandler;
import org.fastlight.apt.handler.FastAspectHandlerBuilder;

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
     * 切面执行器实例
     */
    private FastAspectHandler handler;
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
    private MetaClass ownerType;

    /**
     * 方法参数
     */
    private MetaParameter[] parameters;

    /**
     * 注解
     */
    private MetaAnnotation[] annotations;


    /**
     * handler 的构造器
     */
    private Class<? extends FastAspectHandlerBuilder> builder;

    /**
     * 返回获取当前方法对象，懒汉模式
     */
    private transient Method method;

    /**
     * 全局元数据缓存
     */
    private Map<String, Object> metaExtensions = Maps.newHashMap();

    public int getCacheIndex() {
        return cacheIndex;
    }

    public FastAspectHandler getHandler() {
        return handler;
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

    public MetaClass getOwnerType() {
        return ownerType;
    }

    public MetaParameter[] getParameters() {
        return parameters;
    }

    public MetaAnnotation[] getAnnotations() {
        return annotations;
    }

    public Class<? extends FastAspectHandlerBuilder> getBuilder() {
        return builder;
    }

    /**
     * 反射获取 Method 信息
     *
     * @return 当前 Method
     */
    public Method getMethod() {
        if (method == null) {
            method = MethodUtils.getMatchingMethod(
                    ownerType.getType(),
                    name,
                    Arrays.stream(parameters).map(MetaParameter::getType).toArray(Class<?>[]::new)
            );
        }
        // 有泛型的情况，可能会出现匹配不到，这里只要方法名，方法参数个数，[方法参数名] 相等的唯一匹配即可
        // 因为有泛型，所以就不匹配类型了
        if (method == null) {
            List<Method> methodList = Arrays.stream(ownerType.getType().getDeclaredMethods())
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

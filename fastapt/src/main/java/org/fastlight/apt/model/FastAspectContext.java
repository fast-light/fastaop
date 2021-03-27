package org.fastlight.apt.model;

import com.google.common.collect.Maps;

import javax.annotation.CheckForNull;
import java.util.Map;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@SuppressWarnings("unchecked")
public class FastAspectContext {
    /**
     * 方法元数据
     */
    private MetaMethod metaMethod;

    /**
     * 当前调用的 this，静态方法就是 null
     */
    private Object owner;

    /**
     * 方法的返回值
     */
    private Object returnVal;

    /**
     * 扩展属性，可自定义添加，仅在方法内有效，比如在 preHandle 放入方法的执行时间戳，然后再 postHandle 能算出方法的耗时
     */
    private Map<String, Object> extensions;

    /**
     * 添加扩展属性，方法内有效
     *
     * @param key 扩展 key
     * @param val 扩展 value
     */
    public void addExtension(String key, Object val) {
        if (extensions == null) {
            extensions = Maps.newHashMap();
        }
        extensions.put(key, val);
    }

    /**
     * 获取扩展属性
     *
     * @param key 扩展 key
     * @param <T> 扩展 value 类型
     * @return 扩展 value
     */
    public <T> T getExtension(String key) {
        if (extensions == null) {
            return null;
        }
        return (T) extensions.get(key);
    }

    /**
     * 添加全局的元数据扩展，每个 Method 有唯一的一个缓存池
     *
     * @param key   扩展 key
     * @param value 扩展 value
     */
    public void addMetaExtension(String key, Object value) {
        getMetaMethod().addMetaExtension(key, value);
    }

    /**
     * 获取全局的元数据扩展，每个 Method 有唯一的缓存池
     *
     * @param key 扩展 key
     * @param <T> 扩展 value 的类型
     * @return 扩展 value
     */
    public <T> T getMetaExtension(String key) {
        return getMetaMethod().getMetaExtension(key);
    }

    public Object getReturnVal() {
        return returnVal;
    }

    public void setReturnVal(Object returnVal) {
        this.returnVal = returnVal;
    }

    public MetaMethod getMetaMethod() {
        return metaMethod;
    }

    @CheckForNull
    public Object getOwner() {
        return owner;
    }

    /**
     * 获取当前环境的 this 可能为空
     *
     * @return 调用调用的 this
     */
    @CheckForNull
    public Object getThis() {
        return getOwner();
    }
}

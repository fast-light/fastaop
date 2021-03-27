package org.fastlight.apt.model;

import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@SuppressWarnings("unchecked")
public class FastAspectContext {
    /**
     * 方法的返回值
     */
    private Object returnVal;

    /**
     * 扩展属性，可自定义添加，仅在方法内有效，比如在 preHandle 放入方法的执行时间戳，然后再 postHandle 能算出方法的耗时
     */
    private Map<String, Object> extension;

    /**
     * 添加扩展属性，方法内有效
     *
     * @param key 扩展 key
     * @param val 扩展 value
     */
    public void addExtension(String key, Object val) {
        if (extension == null) {
            extension = Maps.newHashMap();
        }
        extension.put(key, val);
    }

    /**
     * 获取扩展属性
     *
     * @param key 扩展 key
     * @param <T> 扩展 value 类型
     * @return 扩展 value
     */
    public <T> T getExtension(String key) {
        if (extension == null) {
            return null;
        }
        return (T) extension.get(key);
    }

    public Object getReturnVal() {
        return returnVal;
    }

    public void setReturnVal(Object returnVal) {
        this.returnVal = returnVal;
    }
}

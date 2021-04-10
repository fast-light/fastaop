package org.fastlight.apt.model;

/**
 * 调用原始方法 or 切面方法
 *
 * @author ychost@outlook.com
 * @date 2021-04-06
 */
public enum InvokeMethodType {
    /**
     * 调用原始方法
     */
    ORIGIN,
    /**
     * 调用切面方法
     */
    AOP
}

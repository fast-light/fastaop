package org.fastlight.aop.handler;

import org.fastlight.apt.model.MetaMethod;

/**
 * 切面的生命周期回调，主要有 preHandle，returnHandle，errorHandle，postHandle
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public interface FastAspectHandler {

    int DEFAULT_ORDER = 0;

    /**
     * 执行顺序，数字越小越先执行
     *
     * @return 执行顺序
     */
    default int getOrder() {
        return DEFAULT_ORDER;
    }

    /**
     * 环绕处理
     *
     * @param ctx 切面上下文
     * @return 方法返回值
     * @throws Exception 切面执行异常或者方法调用异常
     */
    Object processAround(FastAspectContext ctx) throws Exception;

    /**
     * 切面是否支持该方法，结果会缓存，一般是通过方法的注解来过滤，缓存之后可提高切面执行效率
     *
     * @param metaMethod 方法元数据
     * @return true 切面会被执行
     */
    default boolean support(MetaMethod metaMethod) {
        return true;
    }

}

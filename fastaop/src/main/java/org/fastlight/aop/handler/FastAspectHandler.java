package org.fastlight.aop.handler;

import org.fastlight.aop.model.FastAspectContext;

/**
 * 切面的生命周期回调，主要有 preHandle，returnHandle，errorHandle，postHandle
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public interface FastAspectHandler {

    /**
     * 执行顺序，数字越小越先执行
     *
     * @return 执行顺序
     */
    default int getOrder() {
        return 0;
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
     * 默认是 SPI 代理执行，判断是否还有下个执行器，默认情况下不要覆盖它
     *
     * @param ctx 切面上下文
     * @return true 还能执行
     */
    default boolean hasNextHandler(FastAspectContext ctx) {
        return false;
    }

}

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
     * 是否支持切面调用
     *
     * @param ctx 方法上下文
     * @return 是否支持切入，后面的 preHandle，postHandle，returnHandle 的执行都依赖于它的结果
     */
    boolean support(FastAspectContext ctx);

    /**
     * 方法执行前调用
     *
     * @param ctx 方法上下文
     */
    void preHandle(FastAspectContext ctx);


    /**
     * 可以改变方法的返回值，最好不要覆写该方法，请覆写 returnHandle 即可
     *
     * @param ctx       方法上下文
     * @param returnVal 方法的返回值
     * @param <T>       方法返回的类型
     * @return 经过切面逻辑之后方法的返回数据
     */
    @SuppressWarnings("unchecked")
    default <T> T returnWrapper(FastAspectContext ctx, T returnVal) {
        ctx.setReturnVal(returnVal);
        returnHandle(ctx);
        return (T) ctx.getReturnVal();
    }

    /**
     * 可以覆盖方法的返回值，对于 void 方法不会调用
     *
     * @param ctx 方法上下文
     * @see FastAspectContext#getReturnVal()
     */
    default void returnHandle(FastAspectContext ctx) {

    }

    /**
     * 当方法抛异常的时候会回调该方法
     *
     * @param ctx 方法上下文
     * @param e   方法抛出的异常
     */
    default void errorHandle(FastAspectContext ctx, Throwable e) {
    }

    /**
     * 方法执行完毕之后的回调，无论方法是否有异常都会回调该方法
     *
     * @param ctx 方法上下文
     */
    default void postHandle(FastAspectContext ctx) {
    }

    /**
     * 执行顺序，数字越小越先执行
     *
     * @return 执行顺序
     */
    default int getOrder() {
        return 0;
    }

}

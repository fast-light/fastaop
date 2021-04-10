package org.fastlight.aop.handler;

/**
 * FastAspect 直接调用的 Handler，系统会生成单例模式
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public interface FastAspectHandlerBuilder {
    /**
     * 构造一个切面执行器，会通过 {@link org.fastlight.aop.annotation.FastAspect} 生成的代码直接引用
     *
     * @return 切面执行器
     */
    FastAspectHandler build();
}

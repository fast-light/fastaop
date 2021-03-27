package org.fastlight.aop.handler;

/**
 * 基于 SPI 的切面构造器，系统默认的 builder，里面会去代理执行 SPI 注入进来的切面逻辑
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class FastAspectSpiHandlerBuilder implements FastAspectHandlerBuilder {
    /**
     * SPI 代理执行器
     */
    public static final FastAspectSpiHandler FAST_ASPECT_SPI_HANDLER = FastAspectSpiHandler.getInstance();

    static {
        FAST_ASPECT_SPI_HANDLER.initHandlers();
    }

    @Override
    public FastAspectHandler build() {
        return FAST_ASPECT_SPI_HANDLER;
    }
}

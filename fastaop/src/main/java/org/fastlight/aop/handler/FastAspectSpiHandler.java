package org.fastlight.aop.handler;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import com.google.common.collect.Lists;
import org.fastlight.aop.model.FastAspectContext;

/**
 * 通过 SPI 注入执行器，然后代理调用他们
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class FastAspectSpiHandler implements FastAspectHandler {
    /**
     * SPI 注入进来的执行器
     */
    private final List<FastAspectHandler> spiHandlers = Lists.newArrayList();

    /**
     * 初始化标志
     */
    private volatile boolean isInit = false;

    /**
     * 初始化锁
     */
    private final Object initLock = new Object();

    /**
     * 单例
     */
    public static final FastAspectSpiHandler SINGLETON = new FastAspectSpiHandler();

    /**
     * 只能是单例模式
     */
    protected FastAspectSpiHandler() {

    }

    /**
     * 获取实例，注意没有初始化
     */
    public static FastAspectSpiHandler getInstance() {
        return SINGLETON;
    }

    /**
     * 通过 SPI 注入 Handlers
     */
    public void initHandlers() {
        if (isInit) {
            return;
        }
        synchronized (initLock) {
            if (isInit) {
                return;
            }
            try {
                spiHandlers.clear();
                ServiceLoader<FastAspectHandler> serviceLoader = ServiceLoader.load(FastAspectHandler.class);
                for (FastAspectHandler handler : serviceLoader) {
                    // 防止重复添加
                    if (spiHandlers.stream().noneMatch(v -> v.getClass().equals(handler.getClass()))) {
                        spiHandlers.add(handler);
                    }
                }
                spiHandlers.sort(Comparator.comparingInt(FastAspectHandler::getOrder));
            } finally {
                isInit = true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object processAround(FastAspectContext ctx) throws Exception {
        Integer index = ctx.getMetaMethod().getHandlerIndex();
        if (spiHandlers.size() == index) {
            return ctx.getMetaMethod().getMethod().invoke(ctx.getThis(), ctx.getArgs());
        }
        if (spiHandlers.size() < index) {
            throw new RuntimeException("[FastAop] not find handler for index " + index);
        }
        // 调用链式处理
        Object result = spiHandlers.get(index).processAround(ctx);
        // 没有调用 ctx.proceed() 直接返回结果
        if ((index + 1) != ctx.getMetaMethod().getHandlerIndex()) {
            return result;
        }
        return processAround(ctx);
    }

    @Override
    public boolean hasNextHandler(FastAspectContext ctx) {
        return spiHandlers.size() > ctx.getMetaMethod().getHandlerIndex() + 1;
    }
}

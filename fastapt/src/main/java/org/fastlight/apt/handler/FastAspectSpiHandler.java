package org.fastlight.apt.handler;

import com.google.common.collect.Lists;
import org.fastlight.apt.model.FastAspectContext;
import org.fastlight.core.lambda.action.FastAction1;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 通过 SPI 注入执行器，然后代理调用他们
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class FastAspectSpiHandler implements FastAspectHandler {
    /**
     * support 返回是 true 的 spi handler 缓存
     */
    public static final String EXT_SUPPORT_INDICES = "fast.support_indices";

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
    public boolean support(FastAspectContext ctx) {
        if (spiHandlers.isEmpty()) {
            return false;
        }
        List<Integer> supportIndices = Lists.newArrayList();
        for (int i = 0; i < spiHandlers.size(); i++) {
            if (spiHandlers.get(i).support(ctx)) {
                supportIndices.add(i);
            }
        }
        ctx.addExtension(EXT_SUPPORT_INDICES, supportIndices);
        return supportIndices.size() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preHandle(FastAspectContext ctx) {
        execProxy(ctx, handler -> handler.preHandle(ctx));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void returnHandle(FastAspectContext ctx) {
        execProxy(ctx, handler -> handler.returnHandle(ctx));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void errorHandle(FastAspectContext ctx, Throwable e) {
        execProxy(ctx, handler -> handler.errorHandle(ctx, e));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postHandle(FastAspectContext ctx) {
        execProxy(ctx, handler -> handler.postHandle(ctx));
    }

    /**
     * 回调 spiHandler
     *
     * @param ctx    方法上下文
     * @param action 回调的生命周期
     */
    protected void execProxy(FastAspectContext ctx, FastAction1<FastAspectHandler> action) {
        List<Integer> supportIndices = ctx.getExtension(EXT_SUPPORT_INDICES);
        for (Integer index : supportIndices) {
            action.invoke(spiHandlers.get(index));
        }
    }
}

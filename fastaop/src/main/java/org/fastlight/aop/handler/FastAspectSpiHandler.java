package org.fastlight.aop.handler;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.fastlight.aop.model.FastAspectContext;
import org.fastlight.apt.model.MetaMethod;

/**
 * 通过 SPI 注入执行器，然后代理调用他们
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class FastAspectSpiHandler implements FastAspectHandler {
    /**
     * support 等于 true 的 handler 索引
     */
    public static final String SUPPORT_INDICES = "fast.supportIndices";

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
        if (getSupportIndices(ctx.getMetaMethod()).contains(index)) {
            Object result = spiHandlers.get(index).processAround(ctx);
            // 没有调用 ctx.proceed() 直接返回结果
            if ((index + 1) != ctx.getMetaMethod().getHandlerIndex()) {
                return result;
            }
            // 对于不支持的链路直接跳过提高执行效率
        } else {
            ctx.getMetaMethod().handleNext();
        }
        return processAround(ctx);
    }

    /**
     * 遍历执行器的 support，并缓存 index
     */
    @Override
    public boolean support(MetaMethod metaMethod) {
        Set<Integer> supportIndices = getSupportIndices(metaMethod);
        // handlerIndex 从 -1 开始的所以这里是大于 handlerIndex + 1
        return supportIndices.size() > 0 && spiHandlers.size() > metaMethod.getHandlerIndex() + 1;
    }

    /**
     * 获取支持该方法的切面索引
     */
    protected Set<Integer> getSupportIndices(MetaMethod metaMethod) {
        Set<Integer> supportIndices = metaMethod.getMetaExtension(SUPPORT_INDICES);
        if (supportIndices != null) {
            return supportIndices;
        }
        // metaMethod 是全局的
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (metaMethod) {
            supportIndices = metaMethod.getMetaExtension(SUPPORT_INDICES);
            if (supportIndices != null) {
                return supportIndices;
            }
            supportIndices = Sets.newHashSet();
            for (int i = 0; i < spiHandlers.size(); i++) {
                if (spiHandlers.get(i).support(metaMethod)) {
                    supportIndices.add(i);
                }
            }
            metaMethod.addMetaExtension(SUPPORT_INDICES, supportIndices);
            return supportIndices;
        }
    }
}

package org.fastlight.aop.handler;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.fastlight.apt.model.InvokeMethodType;
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
        List<Integer> supports = getSupportIndices(ctx.getMetaMethod());
        if (supports.size() < ctx.getSupportIndex()) {
            throw new RuntimeException(
                "[FastAop] support index error for method " + ctx.getMetaMethod().getMethod().getName()
            );
        }
        // 调用原始方法
        if (supports.size() == ctx.getSupportIndex()) {
            try {
                // 赋值标志位，递归调用原始方法，然后会调用原始逻辑
                ctx.getMetaMethod().setInvokeMethodType(InvokeMethodType.ORIGIN);
                return ctx.getMetaMethod().getMethod().invoke(ctx.getThis(), ctx.getArgs());
            } finally {
                ctx.getMetaMethod().setInvokeMethodType(InvokeMethodType.AOP);
            }
        }
        // 调用链式处理
        return spiHandlers.get(supports.get(ctx.getSupportIndex())).processAround(ctx);
    }

    /**
     * 遍历执行器的 support，并缓存 index
     */
    @Override
    public boolean support(MetaMethod metaMethod) {
        List<Integer> supportIndices = getSupportIndices(metaMethod);
        // 当前方法有支持的切面逻辑且线程标识为调用下一个切面逻辑
        return supportIndices.size() > 0 && metaMethod.getInvokeMethodType() == InvokeMethodType.AOP;
    }

    /**
     * 获取支持该方法的切面索引
     */
    protected List<Integer> getSupportIndices(MetaMethod metaMethod) {
        List<Integer> supportIndices = metaMethod.getMetaExtension(SUPPORT_INDICES);
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
            supportIndices = Lists.newArrayList();
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

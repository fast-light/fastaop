package org.fastlight.aop.handler;

import java.util.Map;

import com.google.common.collect.Maps;
import org.fastlight.apt.model.MetaMethod;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@SuppressWarnings("unchecked")
public class FastAspectContext {
    /**
     * handler 构造器
     */
    public static final String EXT_META_BUILDER_CLASS = "fast.meta_handler_builder_class";

    /**
     * handler 实例
     */
    public static final String EXT_META_HANDLER = "fast.meta_handler";

    /**
     * 构造 handler 的时候需要线程安全
     */
    public static final Object HANDLER_LOCKER = new Object();

    /**
     * 每个 context 进入切面 handler 的时候都会 copy 一份，然后赋值这个为 handlerIndex
     * 可以解决多线程调用 ctx.proceed() 的问题
     */
    private int supportIndex;

    /**
     * 方法元数据
     */
    private MetaMethod metaMethod;

    /**
     * 当前调用的 this，静态方法就是 null
     * 通过弱引用防止多线程持有 context 造成内存泄漏
     */
    private Object owner;
    /**
     * 方法的入参
     */
    private Object[] args;

    /**
     * 扩展属性，可自定义添加，仅在方法内有效，比如在 preHandle 放入方法的执行时间戳，然后再 postHandle 能算出方法的耗时
     */
    private Map<String, Object> extensions;

    /**
     * 构造一个上下文
     */
    public static FastAspectContext create(MetaMethod metaMethod, Object owner, Object[] args) {
        // 这是系统初始化的，配置准许切面调用
        FastAspectContext ctx = new FastAspectContext(-1);
        ctx.metaMethod = metaMethod;
        ctx.owner = owner;
        ctx.args = args;
        return ctx;
    }

    public FastAspectContext(int supportIndex) {
        this.supportIndex = supportIndex;
    }

    /**
     * 获取入参 map，丢掉了注解
     */
    public Map<String, Object> getParamMap() {
        Map<String, Object> map = Maps.newHashMap();
        for (int i = 0; i < metaMethod.getParameters().length; i++) {
            map.put(metaMethod.getParameters()[i].getName(), args[i]);
        }
        return map;
    }

    /**
     * 构造一个 Handler，线程安全的，通过缓存来优化性能
     */
    protected FastAspectHandler getHandler() {
        try {
            FastAspectHandler handler = getMetaExtension(EXT_META_HANDLER);
            if (handler != null) {
                return handler;
            }
            synchronized (HANDLER_LOCKER) {
                handler = getMetaExtension(EXT_META_HANDLER);
                if (handler != null) {
                    return handler;
                }
                Class<? extends FastAspectHandlerBuilder> builderClass = getMetaExtension(EXT_META_BUILDER_CLASS);
                if (builderClass == null) {
                    builderClass = FastAspectSpiHandlerBuilder.class;
                }
                FastAspectHandlerBuilder builder = builderClass.newInstance();
                handler = builder.build();
                addMetaExtension(EXT_META_HANDLER, handler);
                return handler;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过注解可以替换内部实现，将其引用到局部变量
     */
    public static FastAspectContext currentContext() {
        throw new RuntimeException("[FastAop] context not find @FastAspect");
    }

    /**
     * 添加扩展属性，方法内有效
     *
     * @param key 扩展 key
     * @param val 扩展 value
     */
    public void addExtension(String key, Object val) {
        if (extensions == null) {
            extensions = Maps.newHashMap();
        }
        extensions.put(key, val);
    }

    /**
     * 获取扩展属性
     *
     * @param key 扩展 key
     * @param <T> 扩展 value 类型
     * @return 扩展 value
     */
    public <T> T getExtension(String key) {
        if (extensions == null) {
            return null;
        }
        return (T)extensions.get(key);
    }

    /**
     * 添加全局的元数据扩展，每个 Method 有唯一的一个缓存池
     *
     * @param key   扩展 key
     * @param value 扩展 value
     */
    public void addMetaExtension(String key, Object value) {
        getMetaMethod().addMetaExtension(key, value);
    }

    /**
     * 获取全局的元数据扩展，每个 Method 有唯一的缓存池
     *
     * @param key 扩展 key
     * @param <T> 扩展 value 的类型
     * @return 扩展 value
     */
    public <T> T getMetaExtension(String key) {
        return getMetaMethod().getMetaExtension(key);
    }

    public MetaMethod getMetaMethod() {
        return metaMethod;
    }

    public Object getOwner() {
        return owner;
    }

    /**
     * 获取当前环境的 this 可能为空
     *
     * @return 调用调用的 this
     */

    public Object getThis() {
        return getOwner();
    }

    public Object[] getArgs() {
        return args;
    }

    /**
     * 生成的代码调用入口，也可看做无抛出异常的 proceed
     */
    public Object invoke(Object... args) {
        try {
            return proceed(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 切面 handler 调用入口，仅支持单线程调用
     */
    public Object proceed(Object... args) throws Exception {
        // 调用下一个处理器，handlerIndex 从 -1 开始的
        // 复制一份，防止多线程问题影响到 args 和 handlerIndex
        FastAspectContext ctx = copy(supportIndex + 1);
        if (args.length > 0) {
            ctx.args = args;
        }
        return getHandler().processAround(ctx);
    }

    /**
     * 是否有 handler 进行切面处理
     */
    public boolean support() {
        return getHandler().support(this.metaMethod);
    }

    /**
     * 浅复制一个 context，解决多线程的问题
     */
    protected FastAspectContext copy(int supportIndex) {
        FastAspectContext ctx = new FastAspectContext(supportIndex);
        ctx.args = args;
        ctx.metaMethod = metaMethod;
        ctx.owner = owner;
        return ctx;
    }

    /**
     * spi 处理的 supports 的索引
     */
    protected int getSupportIndex() {
        return supportIndex;
    }

    /**
     * 设置处理器索引，禁止私自调用
     */
    protected void setSupportIndex(int supportIndex) {
        this.supportIndex = supportIndex;
    }
}

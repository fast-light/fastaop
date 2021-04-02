package org.fastlight.aop.model;

import java.util.Map;

import com.google.common.collect.Maps;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.handler.FastAspectHandlerBuilder;
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
     * 当前线程 id
     */
    private final long threadId = Thread.currentThread().getId();

    /**
     * 方法元数据
     */
    private MetaMethod metaMethod;

    /**
     * 当前调用的 this，静态方法就是 null
     */
    private Object owner;

    /**
     * 方法的返回值
     */
    private Object returnVal;

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
        FastAspectContext ctx = new FastAspectContext();
        ctx.metaMethod = metaMethod;
        ctx.owner = owner;
        ctx.args = args;
        return ctx;
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
    public FastAspectHandler getHandler() {
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
                Class<FastAspectHandlerBuilder> builderClass = getMetaExtension(EXT_META_BUILDER_CLASS);
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

    public Object getReturnVal() {
        return returnVal;
    }

    public void setReturnVal(Object returnVal) {
        this.returnVal = returnVal;
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
     * 生成的代码调用入口，仅支持单线程调用！不能切换线程
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
        if (Thread.currentThread().getId() != threadId) {
            throw new RuntimeException("[FastAop] not support proceed in multi thread");
        }
        Integer originIndex = getMetaMethod().getHandlerIndex();
        try {
            if (args.length > 0) {
                this.args = args;
            }
            // 调用下一个 handler 去处理
            this.getMetaMethod().handleNext();
            return getHandler().processAround(this);
        } finally {
            getMetaMethod().handleReset(originIndex);
        }
    }

    /**
     * 是否有 handler 进行切面处理
     */
    public boolean hasNextHandler() {
        return getHandler().hasNextHandler(this);
    }
}

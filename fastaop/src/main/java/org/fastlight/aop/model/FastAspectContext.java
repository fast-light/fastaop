package org.fastlight.aop.model;

import com.google.common.collect.Maps;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.handler.FastAspectHandlerBuilder;
import org.fastlight.apt.model.MetaMethod;

import javax.annotation.CheckForNull;
import java.util.Map;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@SuppressWarnings("unchecked")
public class FastAspectContext {
    /**
     * 缓存扩展 key
     */
    public static final String EXT_META_BUILDER_CLASS = "fast.meta_handler_builder_class";
    public static final String EXT_META_HANDLER = "fast.meta_handler";
    /**
     * 构造 handler 的时候需要线程安全
     */
    public static final Object HANDLER_LOCKER = new Object();

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
     * 控制当前方法是否立刻 return 或者继续执行方法逻辑
     */
    private FastCtrlFlow ctrlFlow = FastCtrlFlow.EXEC_FLOW;

    /**
     * 是否立即返回
     */
    public boolean isFastReturn() {
        return FastCtrlFlow.FAST_RETURN.equals(ctrlFlow);
    }

    /**
     * 立刻返回，如果是 void 则是 return
     */
    public void fastReturn(Object returnVal) {
        setReturnVal(returnVal);
        ctrlFlow = FastCtrlFlow.FAST_RETURN;
    }

    /**
     * 构造一个上下文
     */
    public static FastAspectContext create(
            MetaMethod metaMethod,
            Object owner,
            Object[] args
    ) {
        FastAspectContext ctx = new FastAspectContext();
        ctx.metaMethod = metaMethod;
        ctx.owner = owner;
        ctx.args = args;
        return ctx;
    }

    /**
     * 构造一个 Handler，线程安全的，通过缓存来优化性能
     */
    public FastAspectHandler buildHandler() {
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
        return (T) extensions.get(key);
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

    @CheckForNull
    public Object getOwner() {
        return owner;
    }

    /**
     * 获取当前环境的 this 可能为空
     *
     * @return 调用调用的 this
     */
    @CheckForNull
    public Object getThis() {
        return getOwner();
    }

    public Object[] getArgs() {
        return args;
    }
}

package org.fastlight.fastaop.example.handler;

import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.model.FastAspectContext;
import org.fastlight.apt.model.MetaMethod;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
@FastAspectAround
public class LogHandler implements FastAspectHandler {

    @Override
    public Object processAround(FastAspectContext ctx) throws Exception {
        System.out.printf("[processAround] %s.%s \n", ctx.getMetaMethod().getMetaOwner().getType().getName(),
            ctx.getMetaMethod().getName()
        );
        return ctx.proceed();
    }

    @Override
    public int getOrder() {
        return 1;
    }

    /**
     * 判断是否切入某个方法，该 support 决定了后面的 processAround 是否被调用，结果会被缓存（提高执行效率）
     * 如果想动态调整对某个方法的支持，请返回 true，且在 processAround 进行判断
     * 默认返回为 true
     */
    @Override
    public boolean support(MetaMethod metaMethod) {
        return true;
    }
}

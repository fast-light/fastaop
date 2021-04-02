package org.fastlight.fastaop.example.handler;

import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.model.FastAspectContext;

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
}

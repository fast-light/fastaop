package org.fastlight.fastaop.example.handler;

import org.fastlight.aop.annotation.FastAspectMark;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.model.FastAspectContext;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
@FastAspectMark
public class LogHandler implements FastAspectHandler {
    public static final String START_MS = "log.start";

    @Override
    public boolean support(FastAspectContext ctx) {
        return true;
    }

    @Override
    public void preHandle(FastAspectContext ctx) {
        ctx.addExtension(START_MS, System.currentTimeMillis());
        System.out.printf("[preHandle] [%s.%s] [input]==> %s \n",
                ctx.getMetaMethod().getMetaOwner().getType(),
                ctx.getMetaMethod().getName(), ctx.getParamMap()
        );
    }

    @Override
    public void postHandle(FastAspectContext ctx) {
        Long cost = System.currentTimeMillis() - (Long) ctx.getExtension(START_MS);
        System.out.printf("[postHandle] [%s.%s] [output]==> %s [cost]==> %sms \n",
                ctx.getMetaMethod().getMetaOwner().getType(),
                ctx.getMetaMethod().getName(),
                ctx.getReturnVal(),
                cost
        );
    }
}

package org.fastlight.fastaop.example.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectContext;
import org.fastlight.aop.handler.FastAspectHandler;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
@FastAspectAround(support = LogAccess.class)
public class LogHandler implements FastAspectHandler {

    /**
     * 环绕模式切入方法体
     */
    @Override
    public Object processAround(FastAspectContext ctx) throws Exception {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.printf("[%s] -- [%s.%s]\n", time,
            ctx.getMetaMethod().getMetaOwner().getType().getName(),
            ctx.getMetaMethod().getName()
        );
        // 调用原始方法执行
        return ctx.proceed();
    }
}

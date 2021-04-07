package org.fastlight.fastaop.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectContext;
import org.fastlight.aop.handler.FastAspectHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author ychost@outlook.com
 * @date 2021-04-07
 */
public class CustomDefinedTest {

    private static volatile boolean isLogged = false;

    @Test
    @LogAccess
    public void test() {
        Assert.assertTrue(isLogged);
    }

    @Target(ElementType.METHOD)
    public @interface LogAccess {

    }

    @FastAspectAround(support = LogAccess.class)
    public static class LogAccessHandler implements FastAspectHandler {

        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.printf("[%s] -- %s", time, ctx.getMetaMethod().getName());
            isLogged = true;
            return ctx.proceed();
        }


    }
}

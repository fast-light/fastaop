package com.fastlight.fastaop.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.handler.FastAspectContext;
import org.fastlight.apt.model.MetaAnnotation;
import org.fastlight.apt.model.MetaMethod;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author ychost
 * @date 2021-04-03
 */
@FastAspect
public class SupportTest {
    private static final AtomicInteger SUPPORT_TIMES = new AtomicInteger(0);

    @Test
    public void test() {
        support();
        notSupport();
        Assert.assertEquals(SUPPORT_TIMES.get(), 1);
    }

    @Support
    void support() {

    }

    @Support(value = false)
    void notSupport() {

    }

    @Target(ElementType.METHOD)
    public @interface Support {
        boolean value() default true;
    }

    @FastAspectAround
    public static class SupportAround implements FastAspectHandler {

        /*
         * 只会被执行一次，可提升程序效率
         */
        @Override
        public boolean support(MetaMethod metaMethod) {
            MetaAnnotation annotation = metaMethod.getAnnotation(Support.class);
            return (boolean)Optional.ofNullable(annotation).map(v -> v.getValue("value")).orElse(false);
        }

        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            SUPPORT_TIMES.addAndGet(1);
            return ctx.proceed();
        }
    }
}

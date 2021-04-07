package org.fastlight.fastaop.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.handler.FastAspectContext;
import org.fastlight.apt.model.MetaMethod;
import org.junit.Assert;
import org.junit.Test;

/**
 * 多个切面执行顺序测试
 *
 * @author ychost@outlook.com
 * @date 2021-04-02
 */
@FastAspect
public class MultiAroundTest {
    @Test
    public void test() {
        Assert.assertEquals("m1", aroundTest("m1"));
        Assert.assertEquals("m2", aroundTest("abc"));
    }

    @MultiAround
    public String aroundTest(String m) {
        return "mmmmm";
    }

    @Target(ElementType.METHOD)
    public @interface MultiAround {

    }

    @FastAspectAround
    public static class Around1 implements FastAspectHandler {
        @Override
        public boolean support(MetaMethod metaMethod) {
            return metaMethod.containAnnotation(MultiAround.class);
        }

        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            if (!ctx.getMetaMethod().containAnnotation(MultiAround.class)) {
                return ctx.proceed();
            }
            if ("m1".equals(ctx.getArgs()[0])) {
                return "m1";
            }
            return ctx.proceed();
        }

        @Override
        public int getOrder() {
            return 100;
        }
    }

    @FastAspectAround
    public static class Around2 implements FastAspectHandler {
        @Override
        public boolean support(MetaMethod metaMethod) {
            return metaMethod.containAnnotation(MultiAround.class);
        }

        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            if (!ctx.getMetaMethod().containAnnotation(MultiAround.class)) {
                return ctx.proceed();
            }
            return "m2";
        }

        @Override
        public int getOrder() {
            return 101;
        }
    }
}

package com.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 方法含 lambda 测试
 *
 * @author ychost@outlook.com
 * @date 2021-03-29
 */
@FastAspect
public class LambdaTest {

    @Test
    public void test() {
        String data = supply().get();
        anonymousSupply().get();
    }

    public Supplier<String> supply() {
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        Assert.assertEquals(ctx.getMetaMethod().getMetaOwner().getType(), LambdaTest.class);
        CtxAsserts.assertEq(ctx, LambdaTest.class, "supply");
        return () -> "ok";
    }

    public Supplier<String> anonymousSupply() {
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        CtxAsserts.assertEq(ctx, LambdaTest.class, "anonymousSupply");
        return new Supplier<String>() {
            @Override
            public String get() {
                try {
                    // 方法内部类是不会切入的
                    @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
                    Assert.fail();
                } catch (Exception ignore) {

                }
                return null;
            }
        };
    }
}

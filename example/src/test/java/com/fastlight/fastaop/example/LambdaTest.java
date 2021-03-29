package com.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("ok", data);
    }

    public Supplier<String> supply() {
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        Assert.assertNotNull(ctx);
        Assert.assertEquals(ctx.getMetaMethod().getMetaOwner().getType(), LambdaTest.class);
        return () -> "ok";
    }
}

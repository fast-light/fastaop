package org.fastlight.fastaop.example;

import java.util.concurrent.atomic.AtomicInteger;

import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectContext;
import org.fastlight.aop.handler.FastAspectHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author ychost@outlook.com
 * @date 2021-04-12
 */
public class RecursionTest {
    public static AtomicInteger times = new AtomicInteger(0);

    @Test
    public void test() {
        count(30);
        Assert.assertEquals(30, times.get());
    }

    @TimeCount
    void count(int end) {
        if (end <= 1) {
            return;
        }
        count(--end);
    }

    public @interface TimeCount {

    }

    @FastAspectAround(support = TimeCount.class)
    public static class TimeCountHandler implements FastAspectHandler {

        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            times.addAndGet(1);
            return ctx.proceed();
        }
    }
}

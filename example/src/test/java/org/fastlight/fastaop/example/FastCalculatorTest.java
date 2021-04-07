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
 * 修复一个损坏的计算器
 *
 * @author ychost@outlook.com
 * @date 2021-04-02
 */
@FastAspect
public class FastCalculatorTest {

    /**
     * 单测入口
     */
    @Test
    public void calcTest() {
        int res = add(3, 2);
        Assert.assertEquals(5, res);
    }

    /**
     * 待修复的加法逻辑
     */
    @CalcRepair
    int add(int a, int b) {
        throw new RuntimeException("this is a broken calculator");
    }

    /**
     * 修复注解
     */
    @Target(ElementType.METHOD)
    public @interface CalcRepair {
    }

    /**
     * 修复 calc 的切面
     */
    @FastAspectAround
    public static class CalcRepairHandler implements FastAspectHandler {

        @Override
        public boolean support(MetaMethod metaMethod) {
            return metaMethod.containAnnotation(CalcRepair.class);
        }

        @Override
        public Object processAround(FastAspectContext ctx) throws Exception {
            int a = (int)ctx.getArgs()[0];
            int b = (int)ctx.getArgs()[1];
            return a + b;
        }
    }
}

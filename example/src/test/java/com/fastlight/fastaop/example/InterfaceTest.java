package com.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;
import org.junit.Test;

/**
 * 接口测试
 *
 * @author ychost@outlook.com
 * @date 2021-04-01
 */
public class InterfaceTest {
    @Test
    public void test() {
        new PowerFace() {}.test();
    }

    @FastAspect
    interface PowerFace {
        default void test() {
            @FastAspectVar
            FastAspectContext ctx = FastAspectContext.currentContext();
            CtxAsserts.assertEq(ctx, getClass(), new Object() {}.getClass().getEnclosingMethod());
        }
    }

}

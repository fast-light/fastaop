package com.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 内部类测试，含静态内部类，和动态内部类
 *
 * @author ychost@outlook.com
 * @date 2021-03-29
 */
@FastAspect
public class InnerClassTest {
    @Test
    public void test() {
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        CtxAsserts.assertEq(ctx, InnerClassTest.class, "test");
        InnerStatic innerStatic = new InnerStatic();
        innerStatic.test();
        innerStatic.test(new Object());
        innerStatic.test(new ArrayList<>());
        new InnerDynamic().test();
    }

    @FastAspect
    public static class InnerStatic {
        public void test() {
            @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
            CtxAsserts.assertEq(ctx, getClass(), new Object() {
                    }.getClass().getEnclosingMethod()
            );
        }

        /**
         * 重载 +1
         */
        public void test(Object overload) {
            @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
            CtxAsserts.assertEq(ctx, getClass(), new Object() {
                    }.getClass().getEnclosingMethod(), overload
            );
        }

        /**
         * 重载 +2
         */
        public void test(List<String> overload) {
            @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
            CtxAsserts.assertEq(ctx, getClass(), new Object() {
                    }.getClass().getEnclosingMethod(), overload
            );
        }
    }

    @FastAspect
    public class InnerDynamic {
        public void test() {
            @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
            CtxAsserts.assertEq(ctx, getClass(), "test");
        }
    }
}

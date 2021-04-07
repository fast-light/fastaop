package org.fastlight.fastaop.example;

import org.fastlight.aop.handler.FastAspectContext;
import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author ychost@outlook.com
 * @date 2021-03-30
 */
public class CtxAsserts {
    /**
     * ownerType，method,args 均相等
     */
    public static void assertEq(FastAspectContext ctx, Class<?> ownerType, String methodName, Object... args) {
        // 校验方法正确
        Method method = Arrays.stream(ownerType.getDeclaredMethods()).filter(v -> !v.isBridge())
            .filter(v -> Objects.equals(methodName, v.getName()))
            .limit(1)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("not found method " + ownerType.getName() + "." + methodName));
        assertEq(ctx, ownerType, method, args);
    }

    public static void assertEq(FastAspectContext ctx, Class<?> ownerType, Method method, Object... args) {
        // 静态类没有 this
        if (ctx.getMetaMethod().isStatic()) {
            Assert.assertNull(ctx.getThis());
        } else {
            Assert.assertNotNull(ctx.getThis());
        }
        // 对于生成的匿名类，其 ownerType 为子类
        Assert.assertTrue(ctx.getMetaMethod().getMetaOwner().getType().isAssignableFrom(ownerType));
        Assert.assertArrayEquals(args, ctx.getArgs());
        Assert.assertEquals(method, ctx.getMetaMethod().getMethod());
    }
}

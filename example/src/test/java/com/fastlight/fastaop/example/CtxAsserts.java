package com.fastlight.fastaop.example;

import org.fastlight.aop.model.FastAspectContext;
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
        // 校验父类相等
        Assert.assertEquals(ownerType, ctx.getMetaMethod().getMetaOwner().getType());
        // 校验方法正确
        Method method = Arrays.stream(ownerType.getDeclaredMethods()).filter(v -> !v.isBridge())
                .filter(v -> Objects.equals(methodName, v.getName()))
                .limit(1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found method " + ownerType.getName() + "." + methodName));
        Assert.assertArrayEquals(args, ctx.getArgs());
        Assert.assertEquals(method, ctx.getMetaMethod().getMethod());
    }

}
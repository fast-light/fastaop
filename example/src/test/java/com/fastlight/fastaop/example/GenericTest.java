package com.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;
import org.fastlight.apt.model.MetaMethod;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 泛型方法测试
 *
 * @author ychost@outlook.com
 * @date 2021-03-30
 */
public class GenericTest {
    @Test
    public void bridgeTest() {
        Assert.assertEquals("3", new FastList().get(3));
    }

    @FastAspect
    public static class FastList extends ArrayList<String> {
        @Override
        public String get(int index) {
            // 泛型重载 JVM 会生成两个 get 方法，仅返回类型不一样
            // 通过 isBridge 可以区别
            @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
            List<Method> methods = Arrays.stream(FastList.class.getDeclaredMethods())
                    .filter(v -> v.getName().equals("get"))
                    .collect(Collectors.toList());
            Assert.assertEquals(2, methods.size());
            CtxAsserts.assertEq(ctx, FastList.class, "get", index);
            return String.valueOf(index);
        }
    }
}

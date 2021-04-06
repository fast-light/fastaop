package org.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.handler.FastAspectContext;

/**
 * 一个简单的 FastAop 调用 demo
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class AopExample {
    public static void main(String[] args) {
        String fastAop = hello("[FastAop]");
        System.out.println("hello: " + fastAop);
    }

    @FastAspect
    public static String hello(String name) {
        @FastAspectVar
        FastAspectContext ctx = FastAspectContext.currentContext();
        return String.valueOf(ctx.getArgs()[0]);
    }
}

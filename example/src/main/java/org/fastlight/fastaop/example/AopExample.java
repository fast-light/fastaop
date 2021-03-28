package org.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.model.FastAspectContext;

/**
 * 一个简单的 FastAop 调用 demo
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class AopExample {
    public static void main(String[] args) {
        System.out.println("==>invoked: " + hello("[FastAop]"));
    }

    @FastAspect
    public static String hello(String name) {
        System.out.println("[hello] [input]==> " + name);
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        new FastAspectHandler() {
            @Override
            public boolean support(FastAspectContext ctxs) {
                System.out.println(ctx);
                return false;
            }

            @Override
            public void preHandle(FastAspectContext ctx) {
            }
        };
        return "hello-->>" + ctx.getArgs()[0] + "eq(" + (ctx.getArgs()[0] == name) + ")";
    }
}

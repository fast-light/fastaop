package org.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;

import java.util.function.Supplier;

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
        return "hello-->>" + ctx.getArgs()[0] + "eq(" + (ctx.getArgs()[0] == name) + ")";
    }
}

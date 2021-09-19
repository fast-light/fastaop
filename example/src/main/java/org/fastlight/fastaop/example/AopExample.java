package org.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.handler.FastAspectContext;
import org.fastlight.fastaop.example.handler.LogAccess;

/**
 * 一个简单的 FastAop 调用 demo
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class AopExample {
    @LogAccess
    public static void main(String[] args) {
        @FastAspectVar FastAspectContext context = FastAspectContext.currentContext();
        System.out.println(context.toString());
        System.out.println("[FastAop Hello]");
    }
}

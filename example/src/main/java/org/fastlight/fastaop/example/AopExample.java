package org.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;

import java.util.function.Consumer;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@FastAspect
public class AopExample {

    public static void main(String[] args) {
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        String data = hello("fastaop");
        System.out.println(data);
    }

    @FastAspect
    public static String hello(String name) {
        @FastAspectVar FastAspectContext ctx = FastAspectContext.currentContext();
        System.out.println(ctx);
        new Consumer<String>() {
            @Override
            public void accept(String o) {
                FastAspectContext ctx2 = null;
            }
        };
        return "hello: " + name;
    }

}

package org.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.handler.FastAspectSpiHandlerBuilder;
import org.fastlight.aop.model.FastAspectContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@FastAspect(builder = FastAspectSpiHandlerBuilder.class)
public class AopExample {

    public static void main(String[] args) {
        @FastAspectVar
        FastAspectContext ctx = FastAspectContext.currentContext();
        System.out.println(ctx.getMetaMethod().getMethod());
        main(new ArrayList[]{});
    }

    public static <T extends List> void main(T[] args) {
        @FastAspectVar
        FastAspectContext ctx = FastAspectContext.currentContext();
        System.out.println(ctx.getMetaMethod().getMethod());
    }
}

package org.fastlight.fastaop.example;

import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@FastAspect
public class AopExample {

    public static void main(String[] args) {
        @FastAspectVar
        FastAspectContext ctx = FastAspectContext.currentContext();
        System.out.println(ctx.getMetaMethod().getMethod());
    }

}

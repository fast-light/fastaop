package org.fastlight.aop.annotation;

import org.fastlight.aop.handler.FastAspectHandlerBuilder;
import org.fastlight.aop.handler.FastAspectSpiHandlerBuilder;

import java.lang.annotation.*;

/**
 * 注入切面逻辑，必须实现接口 {@link org.fastlight.aop.handler.FastAspectHandler}
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface FastAspect {

    /**
     * 构造器，最好别变
     */
    Class<? extends FastAspectHandlerBuilder> builder() default FastAspectSpiHandlerBuilder.class;
}

package org.fastlight.aop.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.fastlight.aop.handler.FastAspectHandler;

/**
 * 自动生成 SPI 文件
 *
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@Documented
public @interface FastAspectAround {
    /**
     * 会自动生成方法覆盖 support，只要方法或者类包含该注解就运行标注的 handler
     */
    Class<? extends Annotation> support() default FastNone.class;

    /**
     * 指定切面的拦截顺序，优先取 override 的 order() 方法，然后取这个
     */
    int order() default FastAspectHandler.DEFAULT_ORDER;
}

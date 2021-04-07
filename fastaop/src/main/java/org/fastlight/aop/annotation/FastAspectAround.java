package org.fastlight.aop.annotation;

import java.lang.annotation.*;

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
}

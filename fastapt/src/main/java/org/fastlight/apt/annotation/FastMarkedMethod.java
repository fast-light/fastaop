package org.fastlight.apt.annotation;

import java.lang.annotation.*;

/**
 * 被切的方法上面自动标注的，禁止手工使用
 *
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface FastMarkedMethod {
    /**
     * 静态元数据索引
     */
    int value();
}

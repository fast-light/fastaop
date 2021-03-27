package org.fastlight.apt.annotation;

import java.lang.annotation.*;

/**
 * 能够在方法内部引入切面上下文，含方法名，入参，注解，所在类等信息{@link org.fastlight.apt.model.FastAspectContext}
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.LOCAL_VARIABLE})
@Documented
public @interface FastAspectVar {
}

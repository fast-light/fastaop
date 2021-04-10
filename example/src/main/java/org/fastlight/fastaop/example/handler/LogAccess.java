package org.fastlight.fastaop.example.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 打印方法的调用时间
 * @author ychost@outlook.com
 * @date 2021-04-10
 */
@Target(ElementType.METHOD)
public @interface LogAccess {
}

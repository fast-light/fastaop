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
public @interface FastAspectMark {}

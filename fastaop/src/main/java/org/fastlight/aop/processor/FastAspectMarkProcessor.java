package org.fastlight.aop.processor;

import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.apt.processor.BaseFastSpiProcessor;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
public class FastAspectMarkProcessor extends BaseFastSpiProcessor<FastAspectAround> {

    @Override
    protected Class<?> supportSpiTypes() {
        return FastAspectHandler.class;
    }
}

package org.fastlight.aop.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;
import org.fastlight.apt.processor.BaseAnnotationProcessor;
import org.fastlight.apt.processor.BaseFastProcessor;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.util.List;

/**
 * 注解处理的总入口，java 编译器有个优化，同一个元素只会经过一个 processor，所以这里遍历所有元素并给所有处理器执行
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationProcessor extends BaseAnnotationProcessor {

    List<? extends BaseFastProcessor<?>> processors = Lists.newArrayList(
            new FastAspectProcessor(),
            new FastAspectAroundProcessor(),
            new FastAspectVarProcessor()
    );

    @Override
    protected List<? extends BaseFastProcessor<?>> getProcessors() {
        return processors;
    }
}

package org.fastlight.apt.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;

import javax.annotation.processing.*;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Set;

/**
 * 注解处理的总入口，java 编译器有个优化，同一个元素只会经过一个 processor，所以这里遍历所有元素并给所有处理器执行
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({"*"})
public class AnnotationProcessor extends AbstractProcessor {

    List<? extends BaseFastProcessor<?>> processors = Lists.newArrayList(
            new FastAspectProcessor()
    );

    @Override
    public boolean process(Set<? extends TypeElement> ats, RoundEnvironment env) {
        try {
            if (!env.processingOver()) {
                processAnnotations(ats, env);
            } else {
                processOver();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return true;
    }

    /**
     * roud 完成
     */
    protected void processOver() {
        processors.forEach(BaseFastProcessor::processOver);
    }

    /**
     * 处理注解
     */
    protected void processAnnotations(Set<? extends TypeElement> ats, RoundEnvironment env) {
        processors.forEach(v -> v.processAnnotations(ats, env));
    }

    /**
     * 初始化注入工具
     */
    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        processors.forEach(v -> v.init(environment));
    }
}

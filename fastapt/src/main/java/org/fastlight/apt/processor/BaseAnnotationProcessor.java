package org.fastlight.apt.processor;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * 注解处理的总入口，java 编译器有个优化，同一个元素只会经过一个 processor，所以这里遍历所有元素并给所有处理器执行
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public abstract class BaseAnnotationProcessor extends AbstractProcessor {

    protected abstract List<? extends BaseFastProcessor<?>> getProcessors();

    @Override
    public boolean process(Set<? extends TypeElement> ats, RoundEnvironment env) {
        if (!env.processingOver()) {
            processAnnotations(ats, env);
        } else {
            processOver();
        }
        // 注意由于是处理了 * 所有需要返回 false，否则其它 APT 无法执行
        return false;
    }

    /**
     * roud 完成
     */
    protected void processOver() {
        getProcessors().forEach(BaseFastProcessor::processOver);
    }

    /**
     * 处理注解
     */
    protected void processAnnotations(Set<? extends TypeElement> ats, RoundEnvironment env) {
        getProcessors().forEach(v -> v.processAnnotations(ats, env));
    }

    /**
     * 初始化注入工具
     */
    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        getProcessors().forEach(v -> v.init(environment));
    }
}

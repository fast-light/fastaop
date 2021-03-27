package org.fastlight.apt.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
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

    private void processOver() {

    }

    private void processAnnotations(Set<? extends TypeElement> ats, RoundEnvironment env) {

    }
}

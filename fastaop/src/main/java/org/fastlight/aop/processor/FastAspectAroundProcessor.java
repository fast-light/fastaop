package org.fastlight.aop.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import org.fastlight.aop.annotation.FastAspectAround;
import org.fastlight.aop.annotation.FastNone;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.aop.translator.FastAspectAroundTranslator;
import org.fastlight.apt.processor.BaseFastSpiProcessor;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
public class FastAspectAroundProcessor extends BaseFastSpiProcessor<FastAspectAround> {

    @Override
    protected Class<?> supportSpiTypes() {
        return FastAspectHandler.class;
    }

    @Override
    public void processTypeElement(TypeElement typeElement, AnnotationMirror atm) {
        super.processTypeElement(typeElement, atm);
        Type supportType = getAtValueData(atm, "support");
        // 默认不覆盖 support 方法
        if ((FastNone.class.getName()).equals(supportType.toString())) {
            return;
        }
        FastAspectAroundTranslator translator = getTranslator();
        JCClassDecl jcClassDecl = javacTrees.getTree(typeElement);
        // 对于已经覆写了 support 的就不做变更了
        if (translator.isOverrideSupport(jcClassDecl)) {
            return;
        }
        translator.addSupportMethod(jcClassDecl, supportType);
    }

    public FastAspectAroundTranslator getTranslator() {
        return new FastAspectAroundTranslator(treeMaker, names.table, messager);
    }
}

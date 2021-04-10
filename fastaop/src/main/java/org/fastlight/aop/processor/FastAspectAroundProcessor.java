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
        int order = Integer.parseInt(getAtValueData(atm, "order").toString());
        FastAspectAroundTranslator translator = getTranslator();
        JCClassDecl jcClassDecl = javacTrees.getTree(typeElement);
        // 覆盖 getOrder 和 support 方法
        if (!((FastNone.class.getName()).equals(supportType.toString())
            && !translator.isOverrideSupport(jcClassDecl))) {
            translator.addSupportMethod(jcClassDecl, supportType);
        }
        if (order != FastAspectHandler.DEFAULT_ORDER
            && !translator.isOverrideGetOrder(jcClassDecl)) {
            translator.addGetOrder(jcClassDecl, order);
        }
    }

    public FastAspectAroundTranslator getTranslator() {
        return new FastAspectAroundTranslator(treeMaker, names.table, messager);
    }
}

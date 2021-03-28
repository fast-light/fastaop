package org.fastlight.aop.processor;

import com.google.common.collect.Sets;
import com.sun.tools.javac.tree.JCTree;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.translator.FastAspectVarTranslator;
import org.fastlight.apt.processor.BaseFastProcessor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
public class FastAspectVarProcessor extends BaseFastProcessor<FastAspectVar> {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Sets.newHashSet("*");
    }

    @Override
    public void processExecutableElement(ExecutableElement executableElement, AnnotationMirror atm) {
        JCTree.JCMethodDecl methodDecl = javacTrees.getTree(executableElement);
        TypeElement ownerElement = getOwnerElement(executableElement, TypeElement.class);
        if (ownerElement == null || methodDecl == null) {
            return;
        }
        FastAspectVarTranslator translator = getTranslator();
        methodDecl.accept(translator);
        if (!translator.isAtCtxVar() || translator.isAtAspect()) {
            return;
        }
        translator.checkClass(javacTrees.getTree(ownerElement));
        if (!translator.isAtAspect()) {
            logError(String.format("[FastAop] %s.%s local var @FastAspectVar not match @FastAspect in Method or Class",
                    ownerElement.toString(), executableElement.toString()
            ));
        }
    }

    @Override
    public void processTypeElement(TypeElement typeElement, AnnotationMirror atm) {
        processExecutableOfTypeElement(typeElement, atm, true);
    }

    FastAspectVarTranslator getTranslator() {
        return new FastAspectVarTranslator(treeMaker, names.table, messager);
    }
}

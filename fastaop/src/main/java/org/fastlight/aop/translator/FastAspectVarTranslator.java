package org.fastlight.aop.translator;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.apt.translator.BaseFastTranslator;
import org.fastlight.apt.util.FastCollections;

import javax.annotation.processing.Messager;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
public class FastAspectVarTranslator extends BaseFastTranslator {
    private boolean isAtCtxVar = false;
    private boolean isAtAspect = false;

    public FastAspectVarTranslator(TreeMaker treeMaker, Name.Table names, Messager messager) {
        super(treeMaker, names, messager);
    }

    /**
     * 检查局部变量是否有标注 @FastAspectVar
     */
    @Override
    public void visitAnnotation(JCTree.JCAnnotation jcAnnotation) {
        super.visitAnnotation(jcAnnotation);
        if (jcAnnotation.toString().contains(FastAspectVar.class.getSimpleName() + "(")) {
            isAtCtxVar = true;
        } else if (jcAnnotation.toString().contains(FastAspect.class.getSimpleName() + "(")) {
            isAtAspect = true;
        }
    }

    /**
     * 检查类上面是否有标注 @FastAspect
     */
    public void checkClass(JCTree.JCClassDecl jcClassDecl) {
        if (jcClassDecl.mods == null || FastCollections.isEmpty(jcClassDecl.mods.annotations)) {
            return;
        }
        if (jcClassDecl.mods.annotations.stream().anyMatch(v -> v.toString().contains(FastAspect.class.getSimpleName() + "("))) {
            isAtAspect = true;
        }
    }

    public boolean isAtCtxVar() {
        return isAtCtxVar;
    }

    public boolean isAtAspect() {
        return isAtAspect;
    }
}

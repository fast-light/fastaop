package org.fastlight.apt.processor;

import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import org.fastlight.apt.annotation.FastAspect;

import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Optional;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class FastAspectProcessor extends BaseFastProcessor<FastAspect> {
    @Override
    protected void processExecutableElement(ExecutableElement executableElement, @Nullable AnnotationMirror atm) {
        TypeElement ownerElement = getOwnerElement(executableElement, TypeElement.class);
        JCMethodDecl jcMethodDecl = javacTrees.getTree(executableElement);
        if (ownerElement == null || jcMethodDecl == null) {
            return;
        }
        // 不切构造函数和初始化
        if (!Optional.ofNullable(jcMethodDecl.getReturnType()).map(v -> v.type).isPresent()) {
            return;
        }
        // 不切匿名类
        if (!Optional.ofNullable(jcMethodDecl.sym).map(v -> v.owner).map(v -> v.type).isPresent()) {
            return;
        }

    }

    @Override
    protected void processTypeElement(TypeElement typeElement, @Nullable AnnotationMirror atm) {

    }
}

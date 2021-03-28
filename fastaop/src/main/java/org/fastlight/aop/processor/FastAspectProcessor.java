package org.fastlight.aop.processor;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.translator.FastAspectTranslator;
import org.fastlight.apt.model.compile.MethodCompile;
import org.fastlight.apt.processor.BaseFastProcessor;

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
    public void processExecutableElement(ExecutableElement executableElement, AnnotationMirror atm) {
        if (atm == null) {
            return;
        }
        Symbol.ClassSymbol ownerElement = getOwnerElement(executableElement, Symbol.ClassSymbol.class);
        JCMethodDecl jcMethodDecl = javacTrees.getTree(executableElement);
        if (jcMethodDecl == null || ownerElement == null) {
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
        MethodCompile methodCompile = new MethodCompile();
        methodCompile.setMethodDecl(jcMethodDecl);
        methodCompile.setOwnerElement(ownerElement);
        methodCompile.addExtension("builder", getAtValueData(atm, "builder"));
        methodCompile.setMethodElement(executableElement);
        FastAspectTranslator translator = getTranslator(methodCompile);
        if (translator.isMarkedMethod()) {
            return;
        }
        // 1. 父类元素添加静态缓存变量
        translator.addCacheVar(javacTrees.getTree(ownerElement));
        // 2. 方法内部织入切面代码
        translator.weaveMethod();
        // 3. 处理 return 和 局部变量
        jcMethodDecl.accept(translator);
    }

    /**
     * 获取语法树遍历器
     *
     * @param methodCompile 注入上下文变量
     */
    protected FastAspectTranslator getTranslator(MethodCompile methodCompile) {
        FastAspectTranslator translator = new FastAspectTranslator(treeMaker, names.table, messager);
        translator.init(methodCompile);
        return translator;
    }

    @Override
    public void processTypeElement(TypeElement typeElement, AnnotationMirror atm) {
        processExecutableOfTypeElement(typeElement, atm, false);
    }
}

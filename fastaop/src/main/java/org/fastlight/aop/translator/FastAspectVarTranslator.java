package org.fastlight.aop.translator;

import java.util.Set;
import java.util.stream.Collectors;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import org.apache.commons.lang3.StringUtils;
import org.fastlight.aop.annotation.FastAspect;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.processor.AspectSupportTypes;
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

    static Set<String> AT_SIMPLE_TYPES = AspectSupportTypes.getSupportTypes()
        .stream()
        .map(v -> {
            String[] array = v.split("\\.");
            if (array.length > 0) {
                return StringUtils.trim(array[array.length - 1]);
            }
            return null;
        }).filter(StringUtils::isNotBlank)
        .collect(Collectors.toSet());

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
        }
        // method 层面注解
        // 标注了 @FastAspect 或者用户自定义注解
        for (String type : AT_SIMPLE_TYPES) {
            if (jcAnnotation.toString().contains(type + "(")) {
                isAtAspect = true;
                return;
            }
        }
    }

    /**
     * 检查类上面是否有标注 @FastAspect
     */
    public void checkClass(JCTree.JCClassDecl jcClassDecl) {
        if (jcClassDecl.mods == null || FastCollections.isEmpty(jcClassDecl.mods.annotations)) {
            return;
        }
        // class 层面注解
        // 标注了 @FastAspect 或者用户自定义注解
        for (JCAnnotation jcAnnotation : jcClassDecl.mods.annotations) {
            for (String type : AT_SIMPLE_TYPES) {
                if (jcAnnotation.toString().contains(type + "(")) {
                    isAtAspect = true;
                    return;
                }
            }
        }
    }

    public boolean isAtCtxVar() {
        return isAtCtxVar;
    }

    public boolean isAtAspect() {
        return isAtAspect;
    }
}

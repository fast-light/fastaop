package org.fastlight.aop.translator;

import javax.annotation.processing.Messager;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name.Table;
import org.fastlight.apt.model.MetaMethod;
import org.fastlight.apt.translator.BaseFastTranslator;

/**
 * @author ychost@outlook.com
 * @date 2021-04-07
 */
public class FastAspectAroundTranslator extends BaseFastTranslator {
    public static final String SUPPORT_METHOD = "support";
    public static final String META_METHOD_PARAM = "metaMethod";

    public FastAspectAroundTranslator(TreeMaker treeMaker,
        Table names,
        Messager messager) {
        super(treeMaker, names, messager);
    }

    /**
     * 是否已经覆写了 support 方法
     */
    public boolean isOverrideSupport(JCClassDecl jcClassDecl) {
        for (JCTree def : jcClassDecl.defs) {
            if (def instanceof JCMethodDecl && SUPPORT_METHOD.equals(((JCMethodDecl)def).name.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 新增 support 方法 {@link org.fastlight.aop.handler.FastAspectHandler#support(MetaMethod)}
     * @formatter:off
     * <example>
     *     public boolean support(MetaMethod metaMethod){
     *         return metaMethod.containAnnotationWithOwner(CustomerAnnotation.class)
     *     }
     * </example>
     * @formatter:on
     */
    public void addSupportMethod(JCClassDecl jcClassDecl, Type supportType) {
        if (isOverrideSupport(jcClassDecl)) {
            logWarn(String.format("[FastAop] class %s is contain support method", jcClassDecl.name.toString()));
            return;
        }
        JCVariableDecl metaMethodParam = treeMaker.VarDef(
            treeMaker.Modifiers(Flags.PARAMETER),
            names.fromString(META_METHOD_PARAM),
            memberAccess("org.fastlight.apt.model.MetaMethod"),
            null
        );
        JCExpression supportExpression = treeMaker.Apply(
            List.nil(),
            memberAccess(META_METHOD_PARAM + ".containAnnotationWithOwner"),
            List.of(classLiteral(supportType))
        );
        JCBlock methodBlock = treeMaker.Block(0, List.of(
            treeMaker.Return(supportExpression)
        ));
        JCTree.JCAnnotation override = treeMaker.Annotation(memberAccess("java.lang.Override"), List.nil());
        JCMethodDecl supportMethod = treeMaker.MethodDef(
            treeMaker.Modifiers(Flags.PUBLIC, List.of(override)),
            getNameFromString(SUPPORT_METHOD),
            treeMaker.TypeIdent(TypeTag.BOOLEAN),
            List.nil(),
            List.of(metaMethodParam),
            List.nil(),
            methodBlock,
            null
        );
        jcClassDecl.defs = jcClassDecl.defs.append(supportMethod);
    }
}

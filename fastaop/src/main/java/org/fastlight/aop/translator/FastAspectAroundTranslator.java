package org.fastlight.aop.translator;

import java.util.Optional;

import javax.annotation.processing.Messager;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name.Table;
import org.apache.commons.lang3.StringUtils;
import org.fastlight.aop.handler.FastAspectHandler;
import org.fastlight.apt.model.MetaMethod;
import org.fastlight.apt.translator.BaseFastTranslator;
import org.fastlight.apt.util.FastCollections;

/**
 * @author ychost@outlook.com
 * @date 2021-04-07
 */
public class FastAspectAroundTranslator extends BaseFastTranslator {
    public static final String SUPPORT_METHOD = "support";
    public static final String GET_ORDER_METHOD = "getOrder";
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
        return containMethod(jcClassDecl, SUPPORT_METHOD, m -> {
            if (FastCollections.size(m.params) != 1) {
                return false;
            }
            // 含有一个参数且参数类型是 MetaMethod
            return Optional.of(m.params.get(0)).map(v -> v.vartype)
                .filter(v -> v instanceof JCIdent)
                .map(v -> ((JCIdent)v).sym)
                .map(Symbol::toString)
                .orElse(StringUtils.EMPTY)
                .equals(MetaMethod.class.getName());
        });
    }

    /**
     * 是否已经覆写了 getOrder() 方法
     */
    public boolean isOverrideGetOrder(JCClassDecl jcClassDecl) {
        return containMethod(jcClassDecl, GET_ORDER_METHOD, m -> FastCollections.isEmpty(m.params));
    }

    /**
     * 新增 support 方法 {@link org.fastlight.aop.handler.FastAspectHandler#support(MetaMethod)}
     * @formatter:off
     * <example>
     *     public boolean support(MetaMethod metaMethod){
     *         return metaMethod.isAnnotatedWithOwner(CustomerAnnotation.class)
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
        // 这句必不可少，否者会报错
        // java.lang.AssertionError: Value of x -1
        metaMethodParam.pos = jcClassDecl.pos;
        JCExpression supportExpression = treeMaker.Apply(
            List.nil(),
            memberAccess(META_METHOD_PARAM + ".isAnnotatedWithOwner"),
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

    /**
     * 新增 getOrder 方法 {@link FastAspectHandler#getOrder()}
     *
     * @formatter:off
     * <example>
     *     public int getOrder(){
     *         return 3;
     *     }
     * </example>
     * @formatter:on
     */
    public void addGetOrder(JCClassDecl jcClassDecl, Integer order) {
        if (isOverrideGetOrder(jcClassDecl)) {
            logWarn(String.format("[FastAop] class %s is contain getOrder method", jcClassDecl.name.toString()));
            return;
        }
        JCReturn jcReturn = treeMaker.Return(
            treeMaker.Literal(TypeTag.INT, order)
        );
        JCBlock methodBlock = treeMaker.Block(0, List.of(jcReturn));
        JCTree.JCAnnotation override = treeMaker.Annotation(memberAccess("java.lang.Override"), List.nil());
        JCMethodDecl getOrderMethod = treeMaker.MethodDef(
            treeMaker.Modifiers(Flags.PUBLIC, List.of(override)),
            getNameFromString(GET_ORDER_METHOD),
            treeMaker.TypeIdent(TypeTag.INT),
            List.nil(),
            List.nil(),
            List.nil(),
            methodBlock,
            null
        );
        jcClassDecl.defs = jcClassDecl.defs.append(getOrderMethod);
    }
}

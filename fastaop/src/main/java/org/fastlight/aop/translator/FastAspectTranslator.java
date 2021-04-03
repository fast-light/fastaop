package org.fastlight.aop.translator;

import java.util.Optional;

import javax.annotation.processing.Messager;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name.Table;
import org.fastlight.aop.annotation.FastAspectVar;
import org.fastlight.aop.model.FastAspectContext;
import org.fastlight.apt.model.MetaMethod;
import org.fastlight.apt.translator.BaseFastTranslator;

/**
 * @author ychost
 * @date 2021-02-24
 **/
@SuppressWarnings("unchecked")
public class FastAspectTranslator extends BaseFastTranslator {
    public static final String CONTEXT_VAR = "__fast_context";

    public static final String META_METHOD_VAR = "__fast_meta_method";

    /**
     * 当前 visit 的是否为内部类，可避免对方法内部匿名类的切入
     */
    private boolean isInnerClass = false;

    /**
     * 父元素下面的静态缓存变量
     */
    private JCVariableDecl metaMethodVar = null;

    /**
     * 初始化注入相关元素
     */
    public FastAspectTranslator(TreeMaker treeMaker,
        Table names, Messager messager) {
        super(treeMaker, names, messager);
    }

    @Override
    public void visitAnnotation(JCAnnotation jcAnnotation) {
        super.visitAnnotation(jcAnnotation);
    }

    /**
     * 将切面代码织入方法
     */
    public void weaveMethod() {
        JCMethodDecl jcMethodDecl = ctxCompile.getMethodDecl();
        if (jcMethodDecl.body == null) {
            return;
        }
        // 对于匿名函数类不切
        if (!Optional.ofNullable(jcMethodDecl.sym).map(v -> v.owner).map(v -> v.type).isPresent()) {
            return;
        }
        if (!Optional.ofNullable(jcMethodDecl.name).isPresent()) {
            return;
        }
        if (isMarkedMethod()) {
            return;
        }
        Integer methodIndex = addMetaMethod();
        markMetaMethodAnnotation(methodIndex);
        JCVariableDecl ctxVar = newContextVar(methodIndex);
        ListBuffer<JCStatement> ctxStatements = new ListBuffer<>();
        ctxStatements.add(ctxVar);
        ctxStatements.add(invokeAopStatement());
        changeMethodDefine(jcMethodDecl, statements -> {
                ctxStatements.addAll(statements);
                return ctxStatements.toList();
            }
        );
    }

    /**
     * 生成切面方法调用代码
     * @formatter:off
     * <example>
     *     public void func(Object... args){
     *         FastAspectContext __fast_context = ...
     *         if(__fast_context.support()){
     *             return __fast_context.invoke()
     *         }
     *     }
     * </example>
     * @formatter:on
     * @see FastAspectContext#support()
     */
    protected JCStatement invokeAopStatement() {
        JCExpression hasNextHandler = treeMaker.Apply(
            List.nil(),
            memberAccess(CONTEXT_VAR + ".support"),
            List.nil()
        );

        JCExpression invoke = treeMaker.Apply(
            List.nil(),
            memberAccess(CONTEXT_VAR + ".invoke"),
            List.nil()
        );
        JCBlock invokeBlock;
        if (ctxCompile.canReturn()) {
            JCReturn jcReturn = treeMaker.Return(
                treeMaker.TypeCast(
                    ctxCompile.getReturnType(),
                    invoke
                )
            );
            invokeBlock = treeMaker.Block(0, List.of(jcReturn));
        } else {
            invokeBlock = treeMaker.Block(0, List.of(
                treeMaker.Exec(invoke),
                treeMaker.Return(null)
            ));
        }
        return treeMaker.If(
            hasNextHandler,
            invokeBlock,
            null
        );
    }

    /**
     * 添加 __fast_meta_method 变量
     */
    public void addMetaMethodVar(JCClassDecl jcClassDecl) {
        metaMethodVar = getVar(jcClassDecl.defs, META_METHOD_VAR);
        if (metaMethodVar != null) {
            return;
        }
        JCExpression newArray = treeMaker.NewArray(
            memberAccess(MetaMethod.class.getName()),
            List.nil(),
            List.nil()
        );
        // 添加 变量定义
        metaMethodVar = treeMaker.VarDef(
            treeMaker.Modifiers(getClassFinalModifiers()),
            getNameFromString(META_METHOD_VAR),
            treeMaker.TypeArray(memberAccess(MetaMethod.class.getName())),
            newArray
        );
        addClassVar(jcClassDecl, metaMethodVar);
    }

    /**
     * 用户可直接在方法体内部拿到 __fast_context
     *
     * @see FastAspectContext#currentContext()
     */
    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        super.visitVarDef(jcVariableDecl);
        if (isInnerClass) {
            return;
        }
        // 对于用了 FastAspectContext.currentContext()
        // 或者 @FastAspectVar 的变量统统进行替换
        if (jcVariableDecl.toString().contains("FastAspectContext")
            && ((jcVariableDecl.init != null
            && jcVariableDecl.init.toString().contains("FastAspectContext.currentContext()"))
            || jcVariableDecl.toString().contains(FastAspectVar.class.getSimpleName())
        )) {
            jcVariableDecl.init = memberAccess(CONTEXT_VAR);
        }
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        isInnerClass = true;
        super.visitClassDef(jcClassDecl);
        isInnerClass = false;
    }

    /**
     * 将当前 method 的元元数据进行缓存
     */
    protected Integer addMetaMethod() {
        JCNewArray originInit = (JCNewArray)metaMethodVar.init;
        List<JCExpression> elements = originInit.elems;
        ListBuffer<JCExpression> newElements = new ListBuffer<>();
        newElements.addAll(elements);
        newElements.add(newMetaExpression(elements.size()));
        originInit.elems = newElements.toList();
        return newElements.size() - 1;
    }

    /**
     * @see FastAspectContext#create(MetaMethod, Object, Object[])
     */
    protected JCVariableDecl newContextVar(Integer methodIndex) {
        JCExpression metaExpression = treeMaker.Indexed(memberAccess(META_METHOD_VAR), treeMaker.Literal(methodIndex));
        return treeMaker.VarDef(
            treeMaker.Modifiers(0),
            getNameFromString(CONTEXT_VAR),
            memberAccess(FastAspectContext.class.getName()),
            treeMaker.Apply(
                List.nil(),
                memberAccess(getCreateMethod(FastAspectContext.class)),
                List.of(metaExpression, ownerExpression(), argsExpression(ctxCompile.getMethodDecl()))
            )
        );
    }

    /**
     * 将 builder class 作为 metaExtension 传入
     */
    @Override
    protected JCExpression metaExtensionExpression() {
        return treeMaker.Apply(
            List.nil(),
            newMapMethod,
            List.of(treeMaker.Literal(FastAspectContext.EXT_META_BUILDER_CLASS), builderExpression())
        );
    }

    /**
     * @see MetaMethod#getMetaExtension(String)
     */
    protected JCExpression builderExpression() {
        Type builder = ctxCompile.getExtension("builder");
        return treeMaker.ClassLiteral(builder);
    }

}

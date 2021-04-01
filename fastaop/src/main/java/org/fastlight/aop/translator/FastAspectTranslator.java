package org.fastlight.aop.translator;

import java.util.Optional;

import javax.annotation.processing.Messager;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCIdent;
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
import org.fastlight.aop.handler.FastAspectHandler;
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

    public static final String HANDLER_VAR = "__fast_handler";

    public static final String EXCEPTION_VAR = "__fast_exception";

    public static final String SUPPORT_VAR = "__fast_support";

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
        // 防止重复切入
        if (jcMethodDecl.toString().contains(HANDLER_VAR + ".preHandle")) {
            return;
        }
        if (isMarkedMethod()) {
            return;
        }
        Integer methodIndex = addMetaCache();
        markMetaMethodAnnotation(methodIndex);
        JCVariableDecl ctxVar = newContextVar(methodIndex);
        JCVariableDecl handleVar = handleVar(ctxVar);
        ListBuffer<JCStatement> ctxStatements = new ListBuffer<>();
        ctxStatements.add(ctxVar);
        ctxStatements.add(handleVar);
        ctxStatements.add(supportVar(handleVar));
        ctxStatements.add(preHandleStatement(handleVar));
        changeMethodDefine(jcMethodDecl, statements -> {
                JCStatement bodyStatement = injectTryCatchFinally(
                    statements,
                    null,
                    errorHandleStatement(handleVar),
                    postHandleStatement(handleVar),
                    Throwable.class.getName(),
                    EXCEPTION_VAR,
                    true,
                    jcMethodDecl.pos
                );
                ctxStatements.add(bodyStatement);
                return ctxStatements.toList();
            }
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
     * 处理包装 return，回调 handler 接口
     *
     * @see org.fastlight.aop.handler.FastAspectHandler#returnWrapper(boolean, FastAspectContext, Object)
     */
    @Override
    public void visitReturn(JCReturn jcReturn) {
        super.visitReturn(jcReturn);
        if (isInnerClass) {
            return;
        }
        Type returnType = ctxCompile.getReturnType();
        if (jcReturn.expr == null || returnType == null || "void".equals(returnType.toString())) {
            return;
        }
        // 防止重复处理
        if (jcReturn.expr.toString().contains(CONTEXT_VAR)) {
            return;
        }
        // 对于 lambda 表达式，必须要强转，不然编译报错
        JCExpression castExpr = treeMaker.TypeCast(
            returnType,
            jcReturn.expr
        );
        jcReturn.expr = treeMaker.Apply(
            List.nil(),
            treeMaker.Select(treeMaker.Ident(getNameFromString(HANDLER_VAR)),
                getNameFromString("returnWrapper")),
            List.of(
                treeMaker.Ident(getNameFromString(SUPPORT_VAR)),
                treeMaker.Ident(getNameFromString(CONTEXT_VAR)),
                castExpr
            )
        );
    }

    /**
     * @formatter:off
     * <example>
     * ..
     * catch(Throwable e){
     *     if(__fast_support){
     *         __fast_handler.errorHandle(__fast_context,e);
     *     }
     *     if(__fast_context.isErrorFastReturn()){
     *         return __fast_context.getReturnVal();
     *     }
     *     throw e;
     * }
     * </example>
     * @formatter:on
     * @see org.fastlight.aop.handler.FastAspectHandler#errorHandle(FastAspectContext, Throwable)
     * @see FastAspectContext#isFastReturn()
     */
    protected JCStatement errorHandleStatement(JCVariableDecl handleVar) {
        // void 直接 return
        JCReturn jcReturn = getReturn();
        JCExpression isFastReturn = treeMaker.Apply(
            List.nil(),
            treeMaker.Select(treeMaker.Ident(getNameFromString(CONTEXT_VAR)),
                getNameFromString("isFastReturn")),
            List.nil()
        );
        JCIdent ifSupport = treeMaker.Ident(getNameFromString(SUPPORT_VAR));
        JCStatement errorHandleExec = treeMaker.Exec(
            treeMaker.Apply(
                List.nil(),
                treeMaker.Select(treeMaker.Ident(handleVar.getName()), getNameFromString("errorHandle")),
                List.of(
                    treeMaker.Ident(getNameFromString(CONTEXT_VAR)),
                    treeMaker.Ident(getNameFromString(EXCEPTION_VAR))
                )
            )
        );
        return treeMaker.If(
            ifSupport,
            treeMaker.Block(0, List.of(
                errorHandleExec,
                treeMaker.If(
                    isFastReturn,
                    jcReturn,
                    null)
                )
            ),
            null
        );
    }

    /**
     * 获取返回值 ctx.getReturnVal()
     */
    protected JCReturn getReturn() {
        JCReturn jcReturn = treeMaker.Return(null);
        if ("void".equals(ctxCompile.getReturnType().toString())) {
            return jcReturn;
        }
        jcReturn = treeMaker.Return(
            treeMaker.TypeCast(ctxCompile.getReturnType(),
                treeMaker.Apply(
                    List.nil(),
                    treeMaker.Select(
                        treeMaker.Ident(getNameFromString(CONTEXT_VAR)), getNameFromString("getReturnVal")
                    ),
                    List.nil()
                )
            )
        );
        return jcReturn;
    }

    /**
     * @formatter:off
     * <example>
     * if(__fast_support){
     *     __fast_handler.preHandle(__fast_context);
     *     if(__fast_context.preFastReturn()){
     *         return __fast_context.getReturnVal(); // 对于 void 直接 return;
     *     }
     * }
     * </example>
     * @formatter:on
     * @see org.fastlight.aop.handler.FastAspectHandler#preHandle(FastAspectContext)
     * @see FastAspectContext#isFastReturn()
     * @see FastAspectContext#getReturnVal()
     */
    protected JCStatement preHandleStatement(JCVariableDecl handleVar) {
        JCExpressionStatement preHandleExec = treeMaker.Exec(
            treeMaker.Apply(
                List.nil(),
                treeMaker.Select(treeMaker.Ident(handleVar.getName()), getNameFromString("preHandle")),
                List.of(treeMaker.Ident(getNameFromString(CONTEXT_VAR)))
            )
        );
        JCIdent ifSupport = treeMaker.Ident(getNameFromString(SUPPORT_VAR));
        if (ctxCompile.getReturnType() == null) {
            return treeMaker.If(
                ifSupport,
                preHandleExec,
                null
            );
        }
        // void 直接 return
        JCReturn jcReturn = getReturn();
        JCExpression isFastReturn = treeMaker.Apply(
            List.nil(),
            treeMaker.Select(treeMaker.Ident(getNameFromString(CONTEXT_VAR)),
                getNameFromString("isFastReturn")),
            List.nil()
        );
        return treeMaker.If(
            ifSupport,
            treeMaker.Block(0, List.of(
                preHandleExec,
                treeMaker.If(
                    isFastReturn,
                    jcReturn,
                    null
                )
            )),
            null
        );
    }

    /**
     * boolean __fast_support = __fast_handler.support(__fast_context)
     */
    protected JCVariableDecl supportVar(JCVariableDecl handleVar) {
        return treeMaker.VarDef(
            treeMaker.Modifiers(0),
            getNameFromString(SUPPORT_VAR),
            treeMaker.TypeIdent(TypeTag.BOOLEAN),
            treeMaker.Apply(
                List.nil(),
                treeMaker.Select(treeMaker.Ident(handleVar.getName()), getNameFromString("support")),
                List.of(treeMaker.Ident(getNameFromString(CONTEXT_VAR)))
            )
        );
    }

    /**
     * @see org.fastlight.aop.handler.FastAspectHandler#postHandle(FastAspectContext)
     */
    protected JCStatement postHandleStatement(JCVariableDecl handleVar) {
        return treeMaker.If(
            treeMaker.Ident(getNameFromString(SUPPORT_VAR)),
            treeMaker.Exec(
                treeMaker.Apply(
                    List.nil(),
                    treeMaker.Select(treeMaker.Ident(handleVar.getName()), getNameFromString("postHandle")),
                    List.of(treeMaker.Ident(getNameFromString(CONTEXT_VAR)))
                )
            ), null
        );
    }

    /**
     * @see FastAspectContext#buildHandler()
     */
    protected JCVariableDecl handleVar(JCVariableDecl ctxVar) {
        JCExpression expression = treeMaker.Apply(
            List.nil(),
            treeMaker.Select(treeMaker.Ident(ctxVar.getName()), getNameFromString("buildHandler")),
            List.nil()
        );
        return treeMaker.VarDef(treeMaker.Modifiers(0),
            getNameFromString(HANDLER_VAR),
            memberAccess(FastAspectHandler.class.getName()),
            expression
        );
    }

    /**
     * 将当前 method 的元元数据进行缓存
     */
    protected Integer addMetaCache() {
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

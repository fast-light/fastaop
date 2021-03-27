package org.fastlight.apt.translator;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name.Table;
import org.fastlight.apt.annotation.FastAspectVar;
import org.fastlight.apt.handler.FastAspectHandler;
import org.fastlight.apt.model.*;
import org.fastlight.apt.model.compile.MethodCompile;

import javax.annotation.CheckForNull;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import java.util.Optional;

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

    public static final String META_CACHE_VAR = "__fast_meta_cache";

    protected MethodCompile ctxCompile;

    /**
     * 父元素下面的静态缓存变量
     */
    private JCVariableDecl metaCacheVar = null;

    /**
     * 初始化注入需要的参数
     */
    public void init(MethodCompile context) {
        this.ctxCompile = context;
    }

    /**
     * 初始化注入相关元素
     */
    public FastAspectTranslator(TreeMaker treeMaker,
                                Table names, Messager messager) {
        super(treeMaker, names, messager);
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
        Integer methodIndex = addMetaCache();
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
     * 添加 __fast_meta_cache 变量
     */
    public void addCacheVar(JCClassDecl jcClassDecl) {
        for (JCTree def : jcClassDecl.defs) {
            if (def instanceof JCVariableDecl && META_CACHE_VAR.equals(((JCVariableDecl) def).name.toString())) {
                metaCacheVar = (JCVariableDecl) def;
                break;
            }
        }
        if (metaCacheVar != null) {
            return;
        }
        JCExpression newArray = treeMaker.NewArray(
                memberAccess(MetaMethod.class.getName()),
                List.nil(),
                List.nil()
        );
        // 非静态内部类不能加 static
        long modifiers = Flags.PRIVATE;
        if (!(ctxCompile.getOwnerElement() instanceof PackageElement)
                && !ctxCompile.getOwnerElement().getModifiers().contains(Modifier.STATIC)) {
            modifiers = modifiers | Flags.STATIC;
        }
        // 添加 变量定义
        metaCacheVar = treeMaker.VarDef(
                treeMaker.Modifiers(modifiers),
                getNameFromString(META_CACHE_VAR),
                treeMaker.TypeArray(memberAccess(MetaMethod.class.getName())),
                newArray
        );
        ListBuffer<JCTree> defs = new ListBuffer<>();
        defs.addAll(jcClassDecl.defs);
        defs.add(metaCacheVar);
        jcClassDecl.defs = defs.toList();
    }

    /**
     * 用户可直接在方法体内部拿到 __fast_context
     *
     * @see FastAspectContext#currentContext()
     */
    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        super.visitVarDef(jcVariableDecl);
        // 对于用了 ZeusMethodContext.currentContext()
        // 或者 @ZeusMethodContextVar 的变量统统进行替换
        if (jcVariableDecl.toString().contains("ZeusMethodContext")
                && ((jcVariableDecl.init != null
                && jcVariableDecl.init.toString().contains("FastAspectContext.currentContext()"))
                || jcVariableDecl.toString().contains(FastAspectVar.class.getSimpleName())
        )) {
            jcVariableDecl.init = memberAccess(CONTEXT_VAR);
        }
    }

    /**
     * 处理包装 return，回调 handler 接口
     *
     * @see org.fastlight.apt.handler.FastAspectHandler#returnWrapper(FastAspectContext, Object)
     */
    @Override
    public void visitReturn(JCReturn jcReturn) {
        super.visitReturn(jcReturn);
        Type returnType = ctxCompile.getReturnType();
        if (jcReturn.expr == null || returnType == null || "void".equals(returnType.toString())) {
            return;
        }
        // 防止重复处理
        if (jcReturn.expr instanceof JCConditional && jcReturn.expr.toString().contains(SUPPORT_VAR)) {
            return;
        }
        JCExpression originExpr = jcReturn.expr;
        // 对于 lambda 表达式，必须要强转，不然编译报错
        originExpr = treeMaker.TypeCast(
                returnType,
                originExpr
        );
        jcReturn.expr = treeMaker.Conditional(treeMaker.Ident(getNameFromString(SUPPORT_VAR)),
                treeMaker.TypeCast(
                        returnType,
                        treeMaker.Apply(
                                List.nil(),
                                treeMaker
                                        .Select(treeMaker.Ident(getNameFromString(HANDLER_VAR)),
                                                getNameFromString("returnWrapper")),
                                List.of(treeMaker.Ident(getNameFromString(CONTEXT_VAR)), originExpr)
                        )
                ),
                originExpr
        );
    }

    /**
     * @formatter:off
     * <example>
     * ..
     * catch(Throwable e){
     *     if(__fast_support){
     *     __fast_handler.errorHandle(__fast_context,e);
     *         if(__fast_context.isErrorFastReturn()){
     *             return __fast_context.getReturnVal();
     *         }
     *     }
     *     throw e;
     * }
     * </example>
     * @formatter:on
     * @see org.fastlight.apt.handler.FastAspectHandler#errorHandle(FastAspectContext, Throwable)
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
     *     return __fast_context.getReturnVal(); // 对于 void 直接 return;
     *     }
     * }
     * </example>
     * @formatter:on
     * @see org.fastlight.apt.handler.FastAspectHandler#preHandle(FastAspectContext)
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
     * @see org.fastlight.apt.handler.FastAspectHandler#postHandle(FastAspectContext)
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
     * @see MetaMethod#getBuilder()
     */
    protected JCExpression builderExpression() {
        return treeMaker.ClassLiteral(ctxCompile.getBuilder());
    }

    /**
     * @see MetaMethod#getReturnType()
     */
    protected JCExpression returnTypeExpression() {
        return classLiteral(ctxCompile.getReturnType());
    }

    /**
     * @see MetaMethod#getAnnotations()
     */
    protected JCExpression methodAnnotationsExpression() {
        return createAnnotationArrayExpression(ctxCompile.getMethodElement().getAnnotationMirrors());
    }

    /**
     * @see MetaMethod#getParameters()
     */
    protected JCExpression paramsExpression() {
        return paramsExpression((List<VarSymbol>) ctxCompile.getMethodElement().getParameters());
    }

    /**
     * 将当前 method 的元元数据进行缓存
     */
    protected Integer addMetaCache() {
        JCNewArray originInit = (JCNewArray) metaCacheVar.init;
        List<JCExpression> elements = originInit.elems;
        ListBuffer<JCExpression> newElements = new ListBuffer<>();
        newElements.addAll(elements);
        newElements.add(newMetaExpression(elements.size()));
        originInit.elems = newElements.toList();
        return newElements.size() - 1;
    }


    /**
     * @see FastAspectContext#getOwner()
     */
    @CheckForNull
    protected JCExpression ownerExpression() {
        if (ctxCompile.getMethodElement().getModifiers().contains(Modifier.STATIC)) {
            return treeMaker.Literal(TypeTag.BOT, null);
        }
        return treeMaker.Ident(getNameFromString("this"));
    }

    /**
     * @see org.fastlight.apt.model.MetaClass#create(Object, MetaAnnotation[])
     */
    protected JCExpression metaOwnerExpression() {
        JCExpression ownerType = classLiteral(ctxCompile.getOwnerElement().type);
        JCExpression annotations = createAnnotationArrayExpression(ctxCompile.getOwnerElement().getAnnotationMirrors());
        return treeMaker.Apply(
                List.nil(),
                memberAccess(getCreateMethod(MetaClass.class)),
                List.of(ownerType, annotations)
        );
    }

    /**
     * @see MetaMethod#isStatic()
     */
    protected JCExpression isStaticExpression() {
        if (ctxCompile.getMethodElement().getModifiers().contains(Modifier.STATIC)) {
            return treeMaker.Literal(true);
        }
        return treeMaker.Literal(false);
    }

    /**
     * @see MetaMethod#getName()
     */
    protected JCExpression methodNameExpression() {
        return treeMaker.Literal(ctxCompile.getMethodDecl().name.toString());
    }

    /**
     * @see FastAspectContext#create(MetaMethod, Object, Object[])
     */
    protected JCVariableDecl newContextVar(Integer methodIndex) {
        JCExpression metaExpression = treeMaker.Indexed(memberAccess(META_CACHE_VAR), treeMaker.Literal(methodIndex));
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
     * @see MetaMethod#create(Integer, boolean, String, MetaClass, MetaParameter[], Object, Class, MetaAnnotation[])
     */
    protected JCExpression newMetaExpression(Integer cacheIndex) {
        return treeMaker.Apply(
                List.nil(),
                memberAccess(getCreateMethod(MetaMethod.class)),
                List.of(treeMaker.Literal(cacheIndex),
                        isStaticExpression(),
                        methodNameExpression(),
                        metaOwnerExpression(),
                        paramsExpression(),
                        returnTypeExpression(),
                        builderExpression(),
                        methodAnnotationsExpression()
                )
        );
    }

    /**
     * 获取 xxx.create 方法
     */
    protected String getCreateMethod(Class<?> cls) {
        return cls.getName() + ".create";
    }

}

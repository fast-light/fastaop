package org.fastlight.apt.translator;

import com.google.auto.common.AnnotationMirrors;
import com.google.common.collect.Lists;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Class;
import com.sun.tools.javac.code.Attribute.Enum;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.comp.Flow.AssignAnalyzer;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.fastlight.apt.model.*;
import org.fastlight.apt.model.compile.AnnotationCompile;
import org.fastlight.apt.model.compile.MethodCompile;
import org.fastlight.apt.model.compile.ParameterCompile;
import org.fastlight.core.util.FastMaps;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 基础的语法树的一些操作方法
 *
 * @author ychost
 * @date 2020-12-03
 **/
@SuppressWarnings("ALL")
public abstract class BaseFastTranslator extends TreeTranslator {
    protected final TreeMaker treeMaker;
    protected final Name.Table names;
    protected final Messager messager;

    protected final JCExpression newMapMethod;
    protected final JCExpression createAnnotationMethod;
    protected final JCExpression createParamMethod;

    protected MethodCompile ctxCompile;

    public void init(MethodCompile ctxCompile) {
        this.ctxCompile = ctxCompile;
    }

    public static final Map<String, String> PRIMITIVE_MAP = FastMaps.newHashMapWithPair(
            "int", "java.lang.Integer",
            "double", "java.lang.Double",
            "boolean", "java.lang.Boolean",
            "short", "java.lang.Short",
            "byte", "java.lang.Byte",
            "char", "java.lang.Character",
            "float", "java.lang.Float",
            "long", "java.lang.Long"
    );

    public BaseFastTranslator(TreeMaker treeMaker, Name.Table names, Messager messager) {
        this.treeMaker = treeMaker;
        this.names = names;
        this.messager = messager;
        newMapMethod = memberAccess("org.fastlight.core.util.FastMaps.newHashMapWithPair");
        createAnnotationMethod = memberAccess("org.fastlight.apt.model.MetaAnnotation.create");
        createParamMethod = memberAccess("org.fastlight.apt.model.MetaParameter.create");
    }

    /**
     * system.out.print 这样可以直接转换成语法树
     *
     * @param components system.out.print 这种表达式
     * @return 表达式语法树
     */
    protected JCTree.JCExpression memberAccess(String components) {
        // 必须要包装类型才有效
        String boxType = PRIMITIVE_MAP.get(components.toLowerCase(Locale.ROOT));
        if (boxType != null) {
            components = boxType;
        }
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(getNameFromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, getNameFromString(componentArray[i]));
        }
        return expr;
    }

    /**
     * 把 injectStatement 注入到方法的 try catch finally，同时会给方法注入 try catch finally
     *
     * @param bodyStatements      原有方法体语句
     * @param tryStatement        添加 try 的一行语句
     * @param catchStatement      添加 catch 的一行语句
     * @param finallyStatement    添加 finally 一行语句
     * @param catchExceptionClass catch 的 Exception 类名
     * @param catchVarName        catch 的变量名
     * @param startPos            {@link AssignAnalyzer#visitVarDef(com.sun.tools.javac.tree.JCTree.JCVariableDecl)}
     *                            里面的 trackback 会判断大于 startPos 才会对 jcvariable 初始化生效，主要fix catch 变量
     * @param throwError          catch 住了异常之后是否继续往上面抛，默认 true
     */
    protected JCStatement injectTryCatchFinally(
            @Nonnull List<JCStatement> bodyStatements,
            @Nullable JCStatement tryStatement,
            @Nullable JCStatement catchStatement,
            @Nullable JCStatement finallyStatement,
            @Nullable String catchExceptionClass,
            @Nullable String catchVarName,
            @Nullable Boolean throwError,
            int startPos
    ) {
        // 注入 try 里面的语句
        if (tryStatement != null) {
            bodyStatements = injectStart(bodyStatements, tryStatement);
        }
        List<JCCatch> jcCatches = List.nil();
        if (catchStatement != null && catchExceptionClass != null && catchVarName != null) {
            // 默认继续抛异常
            List<JCStatement> catchStatements = List.of(catchStatement);
            if (!Boolean.FALSE.equals(throwError)) {
                JCThrow jcThrow = treeMaker.Throw(treeMaker.Ident(getNameFromString(catchVarName)));
                catchStatements = catchStatements.append(jcThrow);
            }
            JCVariableDecl catchVar = treeMaker.VarDef(
                    treeMaker.Modifiers(0),
                    getNameFromString(catchVarName),
                    memberAccess(catchExceptionClass),
                    null
            );
            // 很重要！，坑了整整两天，找遍全网，最后 debug 源码才发现
            // @see com.sun.tools.javac.comp.Flow.AssignAnalyzer.visitVarDef
            catchVar.pos = startPos + 1;
            JCCatch jcCatch = treeMaker.Catch(
                    catchVar,
                    treeMaker.Block(0, catchStatements)
            );
            jcCatches = List.of(jcCatch);
        }
        List<JCStatement> finallyStatements = List.nil();
        if (finallyStatement != null) {
            finallyStatements = List.of(finallyStatement);
        }
        return treeMaker.Try(
                treeMaker.Block(0, bodyStatements),
                jcCatches,
                treeMaker.Block(0, finallyStatements)
        );
    }

    /**
     * 把 injectStatement 注入到方法体第一行，构造函数里面有 super 就只能加入到第二行
     *
     * @param bodyStatements  方法体原有的所有语句
     * @param injectStatement 待注入语句
     * @return 插入 statement 之后的所有语句
     */
    protected List<JCStatement> injectStart(
            List<JCStatement> bodyStatements,
            JCStatement injectStatement
    ) {
        return injectStart(bodyStatements, List.of(injectStatement));
    }

    /**
     * 把 injectStatement 注入到方法体第一行，构造函数里面有 super 就只能加入到第二行
     *
     * @param bodyStatements   方法体原有的所有语句
     * @param injectStatements 待注入语句
     * @return 插入 statement 之后的所有语句
     */
    protected List<JCStatement> injectStart(
            List<JCStatement> bodyStatements,
            List<JCStatement> injectStatements
    ) {
        JCStatement callSuper = getCallSuperStatement(bodyStatements);
        // 新方法体
        ListBuffer<JCStatement> statements = new ListBuffer<>();
        int start = 0;
        if (callSuper != null) {
            statements.append(callSuper);
            statements.appendList(injectStatements);
            start = 1;
        } else {
            statements.appendList(injectStatements);
        }
        for (int i = start; i < bodyStatements.size(); i++) {
            statements.append(bodyStatements.get(i));
        }
        return statements.toList();
    }

    /**
     * 把 injectStatement 注入到方法体的最后一行，为了处理 return，try 等情况，这里直接用 try/finally 来实现最后一行
     *
     * @param bodyStatements  原有方法体语句
     * @param injectStatement 待注入语句
     * @return 注入之后的方法体语句
     */
    protected List<JCStatement> injectFinally(
            List<JCStatement> bodyStatements,
            JCStatement injectStatement
    ) {
        JCStatement statement = injectTryCatchFinally(
                bodyStatements,
                null,
                null,
                injectStatement,
                null,
                null,
                true,
                -1
        );
        return List.of(statement);
    }

    /**
     * 获取方法体中 super() 这一行
     *
     * @param statements 方法体语句
     * @return super() 这一行语句，可能为空，只有构造函数才可能有，因为 super() 必须放到第一行
     */
    @CheckForNull
    protected JCStatement getCallSuperStatement(List<JCStatement> statements) {
        JCStatement callSuper = null;
        if (statements.size() > 0) {
            if (isConstructorCall(statements.get(0))) {
                callSuper = statements.get(0);
            }
        }
        return callSuper;
    }

    /**
     * 是否是构造函数
     */
    protected boolean isConstructorCall(final JCStatement statement) {
        if (!(statement instanceof JCExpressionStatement)) {
            return false;
        }
        JCExpression expr = ((JCExpressionStatement) statement).expr;
        if (!(expr instanceof JCMethodInvocation)) {
            return false;
        }
        JCExpression invocation = ((JCMethodInvocation) expr).meth;
        String name;
        if (invocation instanceof JCFieldAccess) {
            name = ((JCFieldAccess) invocation).name.toString();
        } else if (invocation instanceof JCIdent) {
            name = ((JCIdent) invocation).name.toString();
        } else {
            name = "";
        }

        return "super".equals(name) || "this".equals(name);
    }


    /**
     * 改变方法定义，不用考虑 super() 等特殊情况
     *
     * @param jcMethodDecl 方法描述
     * @param define       定义语句
     */
    protected void changeMethodDefine(JCMethodDecl jcMethodDecl,
                                      Function<List<JCStatement>, List<JCStatement>> define) {
        super.visitMethodDef(jcMethodDecl);
        // abstract 或者 interface 不支持切入
        if (jcMethodDecl.body == null) {
            return;
        }
        List<JCStatement> statements = jcMethodDecl.body.getStatements();
        // 不修改 <init>
        for (JCStatement statement : statements) {
            if (statement.toString().contains("<init>") || jcMethodDecl.name.toString().contains("<init>")) {
                return;
            }
        }
        JCStatement callSuper = getCallSuperStatement(statements);
        // super() 必须第一行
        if (callSuper != null) {
            statements = List.from(statements.subList(1, statements.length()));
        }
        statements = define.apply(statements);
        if (callSuper != null) {
            statements = List.of(callSuper).appendList(statements);
        }

        jcMethodDecl.body = treeMaker.Block(0, statements);
    }

    /**
     * 调用 String.valueOf(exp)
     */
    protected JCExpressionStatement stringValueOf(JCExpression expression) {
        return treeMaker.Exec(
                treeMaker.Apply(
                        List.nil(),
                        memberAccess("java.lang.String.valueOf"),
                        List.of(expression)
                )
        );
    }


    /**
     * @see MetaAnnotation#create(java.lang.Class, Map)
     */
    protected JCExpression createAnnotationExpression(AnnotationCompile annotation) {
        ListBuffer<JCExpression> argsExpressions = new ListBuffer<>();
        for (AnnotationCompile.AnnotationInfo info : annotation.getInfos()) {
            argsExpressions.add(treeMaker.Literal(info.getName()));
            argsExpressions.add(literalExpression(info.getValue(), info.getValueType().toString()));
        }
        JCMethodInvocation args = treeMaker.Apply(
                List.nil(),
                newMapMethod,
                argsExpressions.toList()
        );
        return treeMaker.Apply(
                List.nil(),
                createAnnotationMethod,
                List.of(treeMaker.ClassLiteral(annotation.getType()), args)
        );
    }


    /**
     * annotationMirrors -> ZeusMetaAnnotation[] expression，支持 defaultValue
     */
    protected JCExpression createAnnotationArrayExpression(
            java.util.List<? extends AnnotationMirror> annotationMirrors
    ) {
        if (CollectionUtils.isEmpty(annotationMirrors)) {
            return treeMaker.NewArray(
                    memberAccess(MetaAnnotation.class.getName()),
                    List.nil(),
                    List.nil()
            );
        }

        ListBuffer<JCExpression> atCreates = new ListBuffer<>();
        for (AnnotationMirror annotation : annotationMirrors) {
            AnnotationCompile annotationCompile = new AnnotationCompile();
            annotationCompile.setInfos(Lists.newArrayList());
            annotationCompile.setType((Type) annotation.getAnnotationType());
            AnnotationMirrors.getAnnotationValuesWithDefaults(annotation).forEach((k, v) -> {
                MethodSymbol symbol = (MethodSymbol) k;
                AnnotationCompile.AnnotationInfo info = new AnnotationCompile.AnnotationInfo();
                info.setName(k.getSimpleName().toString());
                info.setValue(v.getValue());
                info.setValueType(symbol.type);
                annotationCompile.getInfos().add(info);
            });
            atCreates.add(createAnnotationExpression(annotationCompile));
        }
        return treeMaker.NewArray(
                memberAccess(MetaAnnotation.class.getName()),
                List.nil(),
                atCreates.toList()
        );
    }

    /**
     * 修复掉泛型影响
     */
    protected JCExpression classLiteral(Type type) {
        if (type instanceof ArrayType) {
            if (((ArrayType) type).elemtype.tsym.erasure_field == null) {
                return treeMaker.Literal(Object.class.getName());
            }
            return treeMaker.ClassLiteral(type);
        }
        // 处理泛型
        if (type instanceof TypeVar) {
            type = getTypeFromVar((TypeVar) type);
        }
        if (type == null) {
            return treeMaker.Literal(Object.class.getName());
        }
        if (type.tsym.erasure_field == null) {
            // 处理原生类型
            if (PRIMITIVE_MAP.containsKey(type.toString()) || "void".equals(type.toString())) {
                return treeMaker.ClassLiteral(type);
            }
            // 泛型是不会含有 package 的 . 所以可以直接返回
            if (type.toString().contains(".")) {
                return treeMaker.ClassLiteral(type);
            }
            // 实在解析不了就返回 Object
            return treeMaker.Literal(Object.class.getName());
        }
        return treeMaker.ClassLiteral(type.tsym.erasure_field);
    }

    protected Type getTypeFromVar(TypeVar typeVar) {
        if (typeVar.bound == null) {
            return null;
        }
        if (typeVar.bound instanceof TypeVar) {
            return getTypeFromVar((TypeVar) typeVar.bound);
        }
        return typeVar.bound;
    }

    /**
     * 提取注解的编译信息
     */
    protected List<AnnotationCompile> getAnnotationCompiles(List<JCAnnotation> annotations) {
        if (CollectionUtils.isEmpty(annotations)) {
            return List.nil();
        }
        ListBuffer<AnnotationCompile> metaAnnotations = new ListBuffer<>();
        for (JCAnnotation annotation : annotations) {
            AnnotationCompile metaAnnotation = new AnnotationCompile();
            metaAnnotations.add(metaAnnotation);
            metaAnnotation.setType(annotation.attribute.type);
            metaAnnotation.setInfos(List.nil());
            ListBuffer<AnnotationCompile.AnnotationInfo> annotationInfos = new ListBuffer<>();
            if (CollectionUtils.isEmpty(annotation.attribute.values)) {
                continue;
            }
            for (Pair<MethodSymbol, Attribute> pair : annotation.attribute.values) {
                AnnotationCompile.AnnotationInfo info = new AnnotationCompile.AnnotationInfo();
                info.setName(pair.fst.name.toString());
                info.setValue(pair.snd.getValue());
                info.setValueType(pair.fst.type);
                annotationInfos.add(info);
            }
            metaAnnotation.setInfos(annotationInfos.toList());
        }
        return metaAnnotations.toList();
    }


    /**
     * @see MetaMethod#getParameters()
     */
    protected JCExpression paramsExpression(List<VarSymbol> params) {
        ListBuffer<JCExpression> paramsExpression = new ListBuffer<>();
        for (VarSymbol param : params) {
            JCExpression atCreate = createAnnotationArrayExpression(param.getAnnotationMirrors());
            JCExpression clsExpression = classLiteral(param.type);
            JCExpression paramExpression = treeMaker.Apply(
                    List.nil(),
                    createParamMethod,
                    List.of(
                            treeMaker.Literal(param.getSimpleName().toString()),
                            clsExpression,
                            atCreate
                    )
            );
            paramsExpression.add(paramExpression);
        }
        return treeMaker.NewArray(
                memberAccess(MetaParameter.class.getName()),
                List.nil(),
                paramsExpression.toList()
        );
    }

    /**
     * @param methodDecl
     * @return
     * @see FastAspectContext#getArgs()
     */
    protected JCExpression argsExpression(JCMethodDecl methodDecl) {
        ListBuffer<JCExpression> argsExpressions = new ListBuffer<>();
        for (JCVariableDecl param : methodDecl.getParameters()) {
            argsExpressions.add(treeMaker.Ident(param.name));
        }
        return treeMaker.NewArray(
                memberAccess(Object.class.getName()),
                List.nil(),
                argsExpressions.toList()
        );
    }

    /**
     * @see MetaAnnotation#create(java.lang.Class, Map)
     * @see FastMaps#newHashMapWithPair(Object...)
     */
    protected JCExpression paramCreateExpression(ParameterCompile metaParam) {
        ListBuffer<JCExpression> atCreates = new ListBuffer<>();
        for (AnnotationCompile annotation : metaParam.getAnnotations()) {
            atCreates.add(createAnnotationExpression(annotation));
        }
        JCExpression atsCreatedExpression =
                treeMaker.NewArray(
                        memberAccess(MetaAnnotation.class.getName()),
                        List.nil(),
                        atCreates.toList()
                );
        return treeMaker.Apply(
                List.nil(),
                createParamMethod,
                List.of(
                        treeMaker.Literal(metaParam.getName()),
                        treeMaker.Ident(getNameFromString(metaParam.getName())),
                        treeMaker.ClassLiteral(metaParam.getType()),
                        atsCreatedExpression
                )
        );
    }

    /**
     * 根据 value 和 type 生成表达式
     */
    protected JCExpression literalExpression(Object obj, String type) {
        String value = obj.toString();
        value = value.replaceAll("^\"", "").replaceAll("\"$", "");
        String lowerType = type.toLowerCase(Locale.ROOT).replaceAll("\\(\\)", "");
        // 数组特殊处理
        if (lowerType.contains("[]")) {
            ListBuffer<JCExpression> expressions = new ListBuffer<>();
            type = type.replaceAll("<.*>", "").replace("[]", "");
            if (obj instanceof java.util.List) {
                java.util.List list = (java.util.List) obj;
                for (Object data : list) {
                    expressions.add(literalExpression(data, type));
                }
            } else {
                expressions.add(literalExpression(obj, type));
            }
            type = type.replaceAll("<.*>", "")
                    .replaceAll("\\[\\]", "")
                    .replaceAll("\\(\\)", "");
            return treeMaker.NewArray(
                    // Class<? extend AAAL>[] -> Class
                    memberAccess(type),
                    List.nil(),
                    expressions.toList()
            );
        } else if ("java.lang.string".equals(lowerType)) {
            return treeMaker.Literal(value);
        } else if ("int".equals(lowerType)) {
            return treeMaker.Literal(Integer.valueOf(value));
        } else if ("long".equals(lowerType)) {
            value = value.replaceAll("(?i)l", "");
            return treeMaker.Literal(Long.valueOf(value));
        } else if ("double".equals(lowerType)) {
            value = value.replaceAll("(?i)d", "");
            return treeMaker.Literal(Double.valueOf(value));
        } else if ("float".equals(lowerType)) {
            value = value.replaceAll("(?i)f", "");
            return treeMaker.Literal(Float.valueOf(value));
        } else if ("short".equals(lowerType)) {
            return treeMaker.Literal(Short.valueOf(value));
        } else if ("byte".equals(lowerType)) {
            return treeMaker.Literal(Byte.valueOf(value));
        } else if ("boolean".equals(lowerType)) {
            return treeMaker.Literal(Boolean.valueOf(value));
        } else if (lowerType.contains("java.lang.class")) {
            if (obj instanceof Type) {
                return classLiteral((Type) obj);
            }
            if (obj instanceof Class) {
                return classLiteral(((Class) obj).classType);
            }
            return treeMaker.Literal(value + ".class");
        } else if (obj instanceof Enum) {
            return treeMaker.QualIdent(((Enum) obj).value);
        }
        return treeMaker.Literal(value);
    }


    /**
     * @see MetaMethod#create(Integer, boolean, String, MetaClass, MetaParameter[], Object, MetaAnnotation[], Map)
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
                        methodAnnotationsExpression(),
                        metaExtensionExpression()
                )
        );
    }

    /**
     * 扩展元素
     */
    protected JCExpression metaExtensionExpression() {
        return treeMaker.Apply(List.nil(), newMapMethod, List.nil());
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
     * String -> Name
     *
     * @param str 常规的字符串
     */
    protected Name getNameFromString(String str) {
        return names.fromString(str);
    }

    /**
     * 打印 debug 信息，方便调试
     *
     * @param message 待打印信息
     */
    protected void logDebug(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }

    /**
     * 打印 warn 消息
     *
     * @param message
     */
    protected void logWarn(String message) {
        messager.printMessage(Kind.WARNING, message);
    }

    /**
     * 打印 error 信息，同时终止编译
     *
     * @param message 待打印信息
     */
    protected void logError(String message) {
        messager.printMessage(Kind.ERROR, message);
    }


    /**
     * 获取 xxx.create 方法
     */
    protected String getCreateMethod(java.lang.Class<?> cls) {
        return cls.getName() + ".create";
    }

}

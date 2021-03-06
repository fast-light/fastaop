package org.fastlight.apt.translator;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import com.google.auto.common.AnnotationMirrors;
import com.google.common.collect.Lists;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Class;
import com.sun.tools.javac.code.Attribute.Enum;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.comp.Flow.AssignAnalyzer;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.fastlight.apt.annotation.FastMarkedMethod;
import org.fastlight.apt.model.MetaAnnotation;
import org.fastlight.apt.model.MetaMethod;
import org.fastlight.apt.model.MetaParameter;
import org.fastlight.apt.model.MetaType;
import org.fastlight.apt.model.compile.AnnotationCompile;
import org.fastlight.apt.model.compile.MethodCompile;
import org.fastlight.apt.model.compile.ParameterCompile;
import org.fastlight.apt.util.FastCollections;
import org.fastlight.apt.util.FastMaps;

/**
 * ???????????????????????????????????????
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

    public static final String META_OWNER_VAR = "__fast_meta_owner";

    public void init(MethodCompile ctxCompile) {
        this.ctxCompile = ctxCompile;
    }

    public static final Map<String, String> PRIMITIVE_MAP = FastMaps
        .newHashMapWithPair(
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
        newMapMethod = memberAccess("org.fastlight.apt.util.FastMaps.newHashMapWithPair");
        createAnnotationMethod = memberAccess("org.fastlight.apt.model.MetaAnnotation.create");
        createParamMethod = memberAccess("org.fastlight.apt.model.MetaParameter.create");
    }

    /**
     * system.out.print ????????????????????????????????????
     *
     * @param components system.out.print ???????????????
     * @return ??????????????????
     */
    protected JCTree.JCExpression memberAccess(String components) {
        // ??????????????????????????????
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
     * ?????????????????? @FastAspectMethod ???????????????????????????????????? Method
     *
     * @param methodIndex ????????? __fast_meta_method ????????????
     */
    protected void markMetaMethodAnnotation(Integer methodIndex) {
        if (isMarkedMethod()) {
            return;
        }
        JCAnnotation annotation = treeMaker.Annotation(memberAccess(FastMarkedMethod.class.getName()),
            List.of(treeMaker.Assign(memberAccess("value"), treeMaker.Literal(methodIndex)))
        );
        ListBuffer<JCAnnotation> annotations = new ListBuffer<>();
        annotations.addAll(ctxCompile.getMethodDecl().mods.annotations);
        annotations.add(annotation);
        ctxCompile.getMethodDecl().mods.annotations = annotations.toList();
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????
     */
    public boolean isMarkedMethod() {
        return ctxCompile.getMethodDecl().mods.annotations.stream().filter(v -> v.type != null)
            .anyMatch(v -> v.type.toString().equals(FastMarkedMethod.class.getName()));
    }

    /**
     * ??? injectStatement ?????????????????? try catch finally??????????????????????????? try catch finally
     *
     * @param bodyStatements      ?????????????????????
     * @param tryStatement        ?????? try ???????????????
     * @param catchStatement      ?????? catch ???????????????
     * @param finallyStatement    ?????? finally ????????????
     * @param catchExceptionClass catch ??? Exception ??????
     * @param catchVarName        catch ????????????
     * @param startPos            {@link AssignAnalyzer#visitVarDef(com.sun.tools.javac.tree.JCTree.JCVariableDecl)} ?????????
     *                            trackback ???????????????
     *                            startPos ????????? jcvariable ????????????????????????fix catch ??????
     * @param throwError          catch ??????????????????????????????????????????????????? true
     */
    protected JCStatement injectTryCatchFinally(List<JCStatement> bodyStatements, JCStatement tryStatement,
        JCStatement catchStatement, JCStatement finallyStatement, String catchExceptionClass, String catchVarName,
        Boolean throwError, int startPos) {
        // ?????? try ???????????????
        if (tryStatement != null) {
            bodyStatements = injectStart(bodyStatements, tryStatement);
        }
        List<JCCatch> jcCatches = List.nil();
        if (catchStatement != null && catchExceptionClass != null && catchVarName != null) {
            // ?????????????????????
            List<JCStatement> catchStatements = List.of(catchStatement);
            if (!Boolean.FALSE.equals(throwError)) {
                JCThrow jcThrow = treeMaker.Throw(treeMaker.Ident(getNameFromString(catchVarName)));
                catchStatements = catchStatements.append(jcThrow);
            }
            JCVariableDecl catchVar = treeMaker
                .VarDef(treeMaker.Modifiers(0), getNameFromString(catchVarName), memberAccess(catchExceptionClass),
                    null);
            // ????????????????????????????????????????????????????????? debug ???????????????
            // @see com.sun.tools.javac.comp.Flow.AssignAnalyzer.visitVarDef
            catchVar.pos = startPos + 1;
            JCCatch jcCatch = treeMaker.Catch(catchVar, treeMaker.Block(0, catchStatements));
            jcCatches = List.of(jcCatch);
        }
        List<JCStatement> finallyStatements = List.nil();
        if (finallyStatement != null) {
            finallyStatements = List.of(finallyStatement);
        }
        return treeMaker.Try(treeMaker.Block(0, bodyStatements), jcCatches, treeMaker.Block(0, finallyStatements));
    }

    /**
     * ??? injectStatement ??????????????????????????????????????????????????? super ???????????????????????????
     *
     * @param bodyStatements  ??????????????????????????????
     * @param injectStatement ???????????????
     * @return ?????? statement ?????????????????????
     */
    protected List<JCStatement> injectStart(List<JCStatement> bodyStatements, JCStatement injectStatement) {
        return injectStart(bodyStatements, List.of(injectStatement));
    }

    /**
     * ??? injectStatement ??????????????????????????????????????????????????? super ???????????????????????????
     *
     * @param bodyStatements   ??????????????????????????????
     * @param injectStatements ???????????????
     * @return ?????? statement ?????????????????????
     */
    protected List<JCStatement> injectStart(List<JCStatement> bodyStatements, List<JCStatement> injectStatements) {
        JCStatement callSuper = getCallSuperStatement(bodyStatements);
        // ????????????
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
     * ??? injectStatement ???????????????????????????????????????????????? return???try ??????????????????????????? try/finally ?????????????????????
     *
     * @param bodyStatements  ?????????????????????
     * @param injectStatement ???????????????
     * @return ??????????????????????????????
     */
    protected List<JCStatement> injectFinally(List<JCStatement> bodyStatements, JCStatement injectStatement) {
        JCStatement statement =
            injectTryCatchFinally(bodyStatements, null, null, injectStatement, null, null, true, -1);
        return List.of(statement);
    }

    /**
     * ?????????????????? super() ?????????
     *
     * @param statements ???????????????
     * @return super() ???????????????????????????????????????????????????????????????????????? super() ?????????????????????
     */
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
     * ?????????????????????
     */
    protected boolean isConstructorCall(final JCStatement statement) {
        if (!(statement instanceof JCExpressionStatement)) {
            return false;
        }
        JCExpression expr = ((JCExpressionStatement)statement).expr;
        if (!(expr instanceof JCMethodInvocation)) {
            return false;
        }
        JCExpression invocation = ((JCMethodInvocation)expr).meth;
        String name;
        if (invocation instanceof JCFieldAccess) {
            name = ((JCFieldAccess)invocation).name.toString();
        } else if (invocation instanceof JCIdent) {
            name = ((JCIdent)invocation).name.toString();
        } else {
            name = "";
        }

        return "super".equals(name) || "this".equals(name);
    }

    /**
     * ????????????????????????????????? super() ???????????????
     *
     * @param jcMethodDecl ????????????
     * @param define       ????????????
     */
    protected void changeMethodDefine(JCMethodDecl jcMethodDecl,
        Function<List<JCStatement>, List<JCStatement>> define) {
        super.visitMethodDef(jcMethodDecl);
        // abstract ?????? interface ???????????????
        if (jcMethodDecl.body == null) {
            return;
        }
        List<JCStatement> statements = jcMethodDecl.body.getStatements();
        // ????????? <init>
        for (JCStatement statement : statements) {
            if (statement.toString().contains("<init>") || jcMethodDecl.name.toString().contains("<init>")) {
                return;
            }
        }
        JCStatement callSuper = getCallSuperStatement(statements);
        // super() ???????????????
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
     * ?????? String.valueOf(exp)
     */
    protected JCExpressionStatement stringValueOf(JCExpression expression) {
        return treeMaker
            .Exec(treeMaker.Apply(List.nil(), memberAccess("java.lang.String.valueOf"), List.of(expression)));
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
        JCMethodInvocation args = treeMaker.Apply(List.nil(), newMapMethod, argsExpressions.toList());
        return treeMaker
            .Apply(List.nil(), createAnnotationMethod, List.of(treeMaker.ClassLiteral(annotation.getType()), args));
    }

    /**
     * annotationMirrors -> MetaAnnotation[] expression????????? defaultValue
     */
    protected JCExpression createAnnotationArrayExpression(
        java.util.List<? extends AnnotationMirror> annotationMirrors) {
        if (FastCollections.isEmpty(annotationMirrors)) {
            return treeMaker.NewArray(memberAccess(MetaAnnotation.class.getName()), List.nil(), List.nil());
        }

        ListBuffer<JCExpression> atCreates = new ListBuffer<>();
        for (AnnotationMirror annotation : annotationMirrors) {
            AnnotationCompile annotationCompile = new AnnotationCompile();
            annotationCompile.setInfos(Lists.newArrayList());
            annotationCompile.setType((Type)annotation.getAnnotationType());
            AnnotationMirrors.getAnnotationValuesWithDefaults(annotation).forEach((k, v) -> {
                MethodSymbol symbol = (MethodSymbol)k;
                AnnotationCompile.AnnotationInfo info = new AnnotationCompile.AnnotationInfo();
                info.setName(k.getSimpleName().toString());
                info.setValue(v.getValue());
                info.setValueType(symbol.type);
                annotationCompile.getInfos().add(info);
            });
            atCreates.add(createAnnotationExpression(annotationCompile));
        }
        return treeMaker.NewArray(memberAccess(MetaAnnotation.class.getName()), List.nil(), atCreates.toList());
    }

    /**
     * ?????????????????????
     */
    protected JCExpression classLiteral(Type type) {
        if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType)type;
            if (arrayType.elemtype.tsym.erasure_field != null) {
                return treeMaker.ClassLiteral(type);
            }
            return treeMaker.Literal(Object.class.getName());
        }
        // ??????????????????????????????
        if (type instanceof TypeVar) {
            type = getTypeFromVar((TypeVar)type);
        }
        if (type == null) {
            return treeMaker.Literal(Object.class.getName());
        }
        if (type.tsym.erasure_field == null) {
            // ??????????????????
            if (PRIMITIVE_MAP.containsKey(type.toString()) || "void".equals(type.toString())) {
                return treeMaker.ClassLiteral(type);
            }
            // ?????? <T> ????????????
            if (type.toString().contains("<")) {
                return treeMaker.Literal(((Symbol.ClassSymbol)type.tsym).flatname.toString());
            }
            // ??? package ??????
            if (type instanceof Type.ClassType && type.tsym instanceof Symbol.ClassSymbol) {
                return treeMaker.ClassLiteral(type);
            }
            // ??????????????????????????? Object
            return treeMaker.Literal(Object.class.getName());
        }
        return treeMaker.ClassLiteral(type.tsym.erasure_field);
    }

    protected Type getTypeFromVar(TypeVar typeVar) {
        if (typeVar.bound == null) {
            return null;
        }
        if (typeVar.bound instanceof TypeVar) {
            return getTypeFromVar((TypeVar)typeVar.bound);
        }
        return typeVar.bound;
    }

    /**
     * ???????????????????????????
     */
    protected List<AnnotationCompile> getAnnotationCompiles(List<JCAnnotation> annotations) {
        if (FastCollections.isEmpty(annotations)) {
            return List.nil();
        }
        ListBuffer<AnnotationCompile> metaAnnotations = new ListBuffer<>();
        for (JCAnnotation annotation : annotations) {
            AnnotationCompile metaAnnotation = new AnnotationCompile();
            metaAnnotations.add(metaAnnotation);
            metaAnnotation.setType(annotation.attribute.type);
            metaAnnotation.setInfos(List.nil());
            ListBuffer<AnnotationCompile.AnnotationInfo> annotationInfos = new ListBuffer<>();
            if (FastCollections.isEmpty(annotation.attribute.values)) {
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
            JCExpression paramExpression = treeMaker.Apply(List.nil(), createParamMethod,
                List.of(treeMaker.Literal(param.getSimpleName().toString()), clsExpression, atCreate));
            paramsExpression.add(paramExpression);
        }
        return treeMaker.NewArray(memberAccess(MetaParameter.class.getName()), List.nil(), paramsExpression.toList());
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
        return treeMaker.NewArray(memberAccess(Object.class.getName()), List.nil(), argsExpressions.toList());
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
            treeMaker.NewArray(memberAccess(MetaAnnotation.class.getName()), List.nil(), atCreates.toList());
        return treeMaker.Apply(List.nil(), createParamMethod,
            List.of(treeMaker.Literal(metaParam.getName()), treeMaker.Ident(getNameFromString(metaParam.getName())),
                treeMaker.ClassLiteral(metaParam.getType()), atsCreatedExpression));
    }

    /**
     * ?????? value ??? type ???????????????
     */
    protected JCExpression literalExpression(Object obj, String type) {
        String value = obj.toString();
        value = value.replaceAll("^\"", "").replaceAll("\"$", "");
        String lowerType = type.toLowerCase(Locale.ROOT).replaceAll("\\(\\)", "");
        // ??????????????????
        if (lowerType.contains("[]")) {
            ListBuffer<JCExpression> expressions = new ListBuffer<>();
            type = type.replaceAll("<.*>", "").replace("[]", "");
            if (obj instanceof java.util.List) {
                java.util.List list = (java.util.List)obj;
                for (Object data : list) {
                    expressions.add(literalExpression(data, type));
                }
            } else {
                expressions.add(literalExpression(obj, type));
            }
            type = type.replaceAll("<.*>", "").replaceAll("\\[\\]", "").replaceAll("\\(\\)", "");
            return treeMaker.NewArray(
                // Class<? extend AAAL>[] -> Class
                memberAccess(type), List.nil(), expressions.toList());
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
                return classLiteral((Type)obj);
            }
            if (obj instanceof Class) {
                return classLiteral(((Class)obj).classType);
            }
            return treeMaker.Literal(value + ".class");
        } else if (obj instanceof Enum) {
            return treeMaker.QualIdent(((Enum)obj).value);
        }
        return treeMaker.Literal(value);
    }

    /**
     * @see MetaMethod#create(Integer, MetaType, MetaParameter[], MetaAnnotation[], Map)
     */
    protected JCExpression newMetaExpression(Integer metaIndex) {
        return treeMaker.Apply(
            List.nil(),
            memberAccess(getCreateMethod(MetaMethod.class)),
            List.of(treeMaker.Literal(metaIndex),
                metaOwnerExpression(),
                paramsExpression(),
                methodAnnotationsExpression(),
                metaExtensionExpression())
        );
    }

    /**
     * ????????????
     */
    protected JCExpression metaExtensionExpression() {
        return treeMaker.Apply(List.nil(), newMapMethod, List.nil());
    }

    /**
     * @see FastAspectContext#getOwner()
     */
    protected JCExpression ownerExpression() {
        if (ctxCompile.getMethodElement().getModifiers().contains(Modifier.STATIC)) {
            return treeMaker.Literal(TypeTag.BOT, null);
        }
        return treeMaker.Ident(getNameFromString("this"));
    }

    /**
     * @see MetaType#create(java.lang.Class, MetaAnnotation[])
     */
    protected JCExpression metaOwnerExpression() {
        return memberAccess(META_OWNER_VAR);
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
        return paramsExpression((List<VarSymbol>)ctxCompile.getMethodElement().getParameters());
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
     * ?????? defs ???????????? JcVariableDecl ???????????? name ?????????
     */
    protected JCVariableDecl getVar(List<JCTree> defs, String name) {
        if (FastCollections.isEmpty(defs)) {
            return null;
        }
        return defs.stream().filter(
            v -> v instanceof JCVariableDecl && Objects.equals(name, ((JCVariableDecl)v).name.toString()))
            .findFirst()
            .map(v -> (JCVariableDecl)v)
            .orElse(null);
    }

    /**
     * ???????????? metwOwner ???????????????????????? {@link MetaType}
     */
    public void addMetaOwnerVar(JCClassDecl jcClassDecl) {
        if (getVar(jcClassDecl.defs, META_OWNER_VAR) != null) {
            return;
        }
        //  new Object(){}
        JCNewClass anonymousClass = treeMaker.NewClass(
            null,
            List.nil(),
            memberAccess(Object.class.getName()),
            List.nil(),
            treeMaker.ClassDef(
                treeMaker.Modifiers(0),
                getNameFromString(StringUtils.EMPTY),
                List.nil(),
                null,
                List.nil(),
                List.nil()
            ));

        // new Object(){}.getClass().getEnclosingClass()
        JCExpression getClass = treeMaker.Apply(
            List.nil(),
            treeMaker.Select(anonymousClass, getNameFromString("getClass")),
            List.nil()
        );
        // new Object(){}.getClass().get
        JCExpression ownerTypeExpression = treeMaker.Apply(
            List.nil(),
            treeMaker.Select(getClass, getNameFromString("getEnclosingClass")),
            List.nil()
        );
        JCModifiers modifiers = treeMaker.Modifiers(getClassFinalModifiers());
        JCExpression annotations = createAnnotationArrayExpression(ctxCompile.getOwnerElement().getAnnotationMirrors());
        // ??????????????????
        JCVariableDecl ownerTypeVar = treeMaker.VarDef(
            modifiers,
            getNameFromString(META_OWNER_VAR),
            memberAccess(MetaType.class.getName()),
            treeMaker.Apply(
                List.nil(),
                memberAccess(getCreateMethod(MetaType.class)),
                List.of(ownerTypeExpression, annotations)
            )
        );
        addClassVar(jcClassDecl, ownerTypeVar);
    }

    /**
     * ??????????????????????????????
     */
    protected void addClassVar(JCClassDecl jcClassDecl, JCVariableDecl jcVariableDecl) {
        if (getVar(jcClassDecl.defs, jcClassDecl.name.toString()) != null) {
            logError(String.format("duplicate var %s for lcass %s", jcVariableDecl.name.toString(),
                jcClassDecl.name.toString())
            );
            return;
        }
        jcClassDecl.defs = jcClassDecl.defs.append(jcVariableDecl);
    }

    /**
     * ?????????????????????????????????????????????????????????
     * 1. ?????????????????? private static final
     * 2. ???????????????????????????  private final
     * 3. ?????????????????? static final
     */
    protected long getClassFinalModifiers() {
        long modifiers = Flags.PRIVATE;
        if (ctxCompile.getOwnerElement().isInterface()) {
            modifiers = 0;
        }
        boolean isInnerClass = !(ctxCompile.getOwnerElement().getEnclosingElement() instanceof PackageElement);
        boolean isStaticClass = ctxCompile.getOwnerElement().getModifiers().contains(Modifier.STATIC);
        if (!isInnerClass || isStaticClass) {
            modifiers = modifiers | Flags.STATIC;
        }
        modifiers = modifiers | Flags.FINAL;
        return modifiers;
    }

    /**
     * ???????????????????????????????????? checker ????????????
     */
    public boolean containMethod(JCClassDecl jcClassDecl, String method, Function<JCMethodDecl, Boolean> checker) {
        for (JCTree def : jcClassDecl.defs) {
            if (def instanceof JCMethodDecl && method.equals(((JCMethodDecl)def).name.toString())) {
                if (checker != null && Boolean.TRUE.equals(checker.apply((JCMethodDecl)def))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * String -> Name
     *
     * @param str ??????????????????
     */
    protected Name getNameFromString(String str) {
        return names.fromString(str);
    }

    /**
     * ?????? debug ?????????????????????
     *
     * @param message ???????????????
     */
    protected void logDebug(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }

    /**
     * ?????? warn ??????
     *
     * @param message
     */
    protected void logWarn(String message) {
        messager.printMessage(Kind.WARNING, message);
    }

    /**
     * ?????? error ???????????????????????????
     *
     * @param message ???????????????
     */
    protected void logError(String message) {
        messager.printMessage(Kind.ERROR, message);
    }

    /**
     * ?????? xxx.create ??????
     */
    protected String getCreateMethod(java.lang.Class<?> cls) {
        return cls.getName() + ".create";
    }

}

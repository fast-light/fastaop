package org.fastlight.apt.processor;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreElements;
import com.google.common.collect.Sets;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import org.fastlight.apt.util.FastCollections;
import org.fastlight.apt.util.ReflectUtils;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 提供一些注解处理的基础方法
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@SuppressWarnings({"unchecked", "UnstableApiUsage", "SameParameterValue"})
public abstract class BaseFastProcessor<T extends Annotation> {
    /**
     * 编译时输出日志
     */
    public Messager messager;

    /**
     * 提取处理元素的语法树
     */
    public JavacTrees javacTrees;

    /**
     * 语法树处理工具，能够快速生成语法树节点
     */
    public TreeMaker treeMaker;

    /**
     * 用于构建一些标识符，treeMaker 会用到
     */
    public Names names;

    /**
     * 处理的全局上下文，可存储一些标志位
     */
    public Context context;

    /**
     * 可以用来输出文件等等
     */
    public ProcessingEnvironment environment;

    /**
     * 子类继承的泛型
     */
    protected Class<T> atClass;

    /**
     * 初始化，注入所需要的所有元素
     *
     * @param environment 所需要的元素都是通过 env 生成的
     */
    public synchronized void init(ProcessingEnvironment environment) {
        if (!(environment instanceof JavacProcessingEnvironment)) {
            return;
        }
        this.environment = environment;
        this.context = ((JavacProcessingEnvironment) environment).getContext();
        this.names = Names.instance(this.context);
        this.messager = environment.getMessager();
        this.treeMaker = TreeMaker.instance(this.context);
        this.javacTrees = JavacTrees.instance(environment);
        this.atClass = (Class<T>) ReflectUtils.getGenericTypes(getClass())[0];
    }

    /**
     * env round 处理完毕回调，一般可用于注解处理完成统一生成文件等功能
     */
    public void processOver() {

    }

    /**
     * 注解处理入口，这里仅处理 Method 和 Class，要处理其他的元素请 Override
     *
     * @param ats      元素的注解信息，可能为空
     * @param roundEnv 处理工具，能感知到注解相关的元素信息
     */
    public void processAnnotations(Set<? extends TypeElement> ats, RoundEnvironment roundEnv) {
        // 默认情况下仅取出 atClass 相关的元素
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(atClass);
        Set<String> supports = getSupportedAnnotationTypes();
        // 支持任何元素
        if (supports.contains("*")) {
            elements = roundEnv.getRootElements();
        }
        if (FastCollections.isEmpty(elements)) {
            return;
        }
        for (Element element : elements) {
            AnnotationMirror atm = getAtMirror(element, atClass);
            // 要么注解存在与该元素，要么支持处理任意元素
            if (!supports.contains("*") && atm == null) {
                continue;
            }
            if (element instanceof ExecutableElement) {
                processExecutableElement((ExecutableElement) element, atm);
            } else if (element instanceof TypeElement) {
                processTypeElement((TypeElement) element, atm);
            }
        }
    }

    /**
     * 方法，构造函数，初始化语句都会进行到这里进行处理
     *
     * @param executableElement method,constructor,initializer etc..
     * @param atm               注解元素
     */
    public abstract void processExecutableElement(ExecutableElement executableElement, AnnotationMirror atm);

    /**
     * 处理标注在类/接口上面的元素
     *
     * @param typeElement class,interface 元素
     * @param atm         注解元素
     */
    public abstract void processTypeElement(TypeElement typeElement, AnnotationMirror atm);

    /**
     * 处理 TypeElement 下面的 ExecutableElement，支持递归，常用语仅对 Method 进行改造，不对 Field 改造
     *
     * @param typeElement class 或者 interface 元素
     * @param atm         注解元素
     * @param recursive   是否递归处理，及对子类进行一样的处理
     */
    public void processExecutableOfTypeElement(TypeElement typeElement, AnnotationMirror atm, boolean recursive) {
        List<? extends Element> elements = typeElement.getEnclosedElements();
        for (Element element : elements) {
            if (element instanceof ExecutableElement) {
                // 防止重复处理，即 class 和 method 同时标注了某个注解
                // method 的注解会执行的，所以在 class 的处理上就不处理了
                if (element.getAnnotation(atClass) == null) {
                    processExecutableElement((ExecutableElement) element, atm);
                }
                // 递归处理，这里一般是处理子类
            } else if (recursive && element instanceof TypeElement) {
                processTypeElement((TypeElement) element, atm);
            }
        }
    }

    /**
     * 获取 element 上面的注解信息，注解的类型是 atClass
     *
     * @param element 被注解的元素
     * @param atClass 注解类型
     * @return 注解元素，如果没找到就返回 null
     */
    public AnnotationMirror getAtMirror(Element element, Class<? extends Annotation> atClass) {
        return MoreElements.getAnnotationMirror(element, atClass).orNull();
    }

    /**
     * 获取当前处理器支持的注解，优先走 {@link SupportedAnnotationTypes}，然后走继承的注解泛型
     */
    public Set<String> getSupportedAnnotationTypes() {
        SupportedAnnotationTypes sat = this.getClass().getAnnotation(SupportedAnnotationTypes.class);
        if (Optional.ofNullable(sat).map(SupportedAnnotationTypes::value).filter(v -> v.length > 0).isPresent()) {
            return Sets.newHashSet(sat.value());
        }
        if (atClass == null) {
            atClass = (Class<T>) ReflectUtils.getGenericTypes(getClass())[0];
        }
        return Sets.newHashSet(atClass.getName());
    }

    /**
     * 获取父元素，即包装它的元素，这里仅仅做了强转的判空
     */
    public <M> M getOwnerElement(Element element, Class<M> ownerClass) {
        if (element == null) {
            return null;
        }
        Element ownerElement = element.getEnclosingElement();
        if (ownerElement == null) {
            return null;
        }
        if (ownerClass.isAssignableFrom(ownerElement.getClass())) {
            return (M) ownerElement;
        }
        return null;
    }

    /**
     * 获取注解元素的某个属性的值
     *
     * @param atm   注解元素
     * @param field 注解里面的字段名字
     * @param <M>   字段类型
     * @return 字段的值
     */
    public <M> M getAtValueData(AnnotationMirror atm, String field) {
        if (atm == null) {
            return null;
        }
        AnnotationValue atv = AnnotationMirrors.getAnnotationValue(atm, field);
        return (M) Optional.ofNullable(atv).map(AnnotationValue::getValue).orElse(null);
    }

    /**
     * 打印 error 信息，同时终止编译
     *
     * @param message 待打印信息
     */
    protected void logError(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

}

package org.fastlight.apt.model.compile;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import javax.lang.model.element.ExecutableElement;

/**
 * 保存编译过程中的方法信息
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public class MethodCompile {
    /**
     * 父元素
     */
    private Symbol.ClassSymbol ownerElement;


    /**
     * 方法语法树
     */
    private JCMethodDecl methodDecl;

    /**
     * 方法元素
     */
    private ExecutableElement methodElement;

    /**
     * 构造器，{@link org.fastlight.apt.model.FastAspectContext} 会用到
     */
    private Type builder;

    public Type getReturnType() {
        return methodDecl.getReturnType().type;
    }


    public Symbol.ClassSymbol getOwnerElement() {
        return ownerElement;
    }

    public void setOwnerElement(Symbol.ClassSymbol ownerElement) {
        this.ownerElement = ownerElement;
    }

    public JCMethodDecl getMethodDecl() {
        return methodDecl;
    }

    public void setMethodDecl(JCMethodDecl methodDecl) {
        this.methodDecl = methodDecl;
    }

    public Type getBuilder() {
        return builder;
    }

    public void setBuilder(Type builder) {
        this.builder = builder;
    }

    public ExecutableElement getMethodElement() {
        return methodElement;
    }

    public void setMethodElement(ExecutableElement methodElement) {
        this.methodElement = methodElement;
    }
}

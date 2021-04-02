package org.fastlight.apt.model.compile;

import com.google.common.collect.Maps;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import javax.lang.model.element.ExecutableElement;

import java.util.Map;

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
     * 扩展元素
     */
    private Map<String, Object> extensions = Maps.newHashMap();

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

    public ExecutableElement getMethodElement() {
        return methodElement;
    }

    public void setMethodElement(ExecutableElement methodElement) {
        this.methodElement = methodElement;
    }

    public void addExtension(String key, Object value) {
        extensions.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtension(String key) {
        return (T)extensions.get(key);
    }

    public boolean canReturn() {
        if (methodDecl.getReturnType() == null || methodDecl.getReturnType().type == null) {
            return false;
        }
        return !"void".equals(methodDecl.getReturnType().type.toString());
    }
}

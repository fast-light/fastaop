package org.fastlight.apt.model;

/**
 * 控制程序流程
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public enum FastCtrlFlow {
    /**
     * 继续向下运行
     */
    EXEC_FLOW,

    /**
     * 立即返回
     */
    FAST_RETURN,

    /**
     * 立刻抛出异常
     */
    THROW_ERROR;

}

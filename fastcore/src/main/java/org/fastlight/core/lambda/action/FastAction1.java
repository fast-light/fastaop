package org.fastlight.core.lambda.action;

import java.io.Serializable;

/**
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
public interface FastAction1<I> extends Serializable {
    /**
     * 一个入参，无出参
     *
     * @param i
     */
    void invoke(I i);
}

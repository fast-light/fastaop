package org.fastlight.apt.util;

import java.util.Collection;

/**
 * @author ychost@outlook.com
 * @date 2021-03-28
 */
public class FastCollections {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static int size(Collection<?> collection) {
        if (isEmpty(collection)) {
            return 0;
        }
        return collection.size();
    }
}

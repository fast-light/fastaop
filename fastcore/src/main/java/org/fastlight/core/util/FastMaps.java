package org.fastlight.core.util;

import java.util.HashMap;

/**
 * 提供一些便捷的 Map 工具方法
 *
 * @author ychost@outlook.com
 * @date 2021-03-27
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FastMaps {
    /**
     * 将数组转成 map，k1,v2,k2,v2
     *
     * @param kvPairs kv 数组
     * @return map 结果
     */
    public static HashMap newHashMapWithPair(Object... kvPairs) {
        HashMap<Object, Object> map = new HashMap<>();
        if (kvPairs.length % 2 != 0) {
            throw new RuntimeException("[newHashMapWithPair] kvPairs.length error");
        }
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put(kvPairs[i], kvPairs[i + 1]);
        }
        return map;
    }
}

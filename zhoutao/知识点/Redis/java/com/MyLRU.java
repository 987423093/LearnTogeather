package com;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2020/11/5 11:24
 * @CopyRight: 2020 hundsun all rights reserved.
 */
public class MyLRU<K, V> extends LinkedHashMap<K, V> {

    /**
     * 构造方法，设置accessOrder为true，当get的时候放到链表的尾部
     * @param initialCapacity
     */
     public MyLRU(int initialCapacity) {
        super((int) (initialCapacity * 0.75), initialCapacity, true);
     }

    /**
     * 当insert的时候用来判断是否要remove最前面节点的条件
     * @param eldest
     * @return
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > 100;
    }
}

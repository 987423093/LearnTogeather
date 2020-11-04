package com;

import java.util.BitSet;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2020/11/4 17:22
 * @CopyRight: 2020 hundsun all rights reserved.
 */
public class MyBloomFilter {

    // 最大容量
    private static final int DEFAULT_SIZE = 1 << 30;
    // 多种哈希算法的hash key
    private static final int[] SEEDS = new int[]{5, 7, 11, 13, 17, 31, 37, 41};
    // 哈希算法的具体实现
    private SimpleHash[] simpleHashes = new SimpleHash[SEEDS.length];
    // 位运算结果存储
    private BitSet bitSet = new BitSet();

    public MyBloomFilter() {
        for (int i = 0; i < SEEDS.length; i++) {
            simpleHashes[i] = new SimpleHash(DEFAULT_SIZE, SEEDS[i]);
        }
    }

    /**
     * 添加
     *
     * @param value
     */
    public void add(String value) {
        for (SimpleHash simpleHash : simpleHashes) {
            bitSet.set(simpleHash.hash(value), true);
        }
    }

    /**
     * 判断是否包含
     *
     * @param value
     * @return
     */
    public boolean contains(String value) {
        if (value == null) {
            return false;
        }
        boolean result = true;
        for (SimpleHash simpleHash : simpleHashes) {
            result = bitSet.get(simpleHash.hash(value)) & result;
        }
        return result;
    }

    /**
     * 内部类
     */
    public class SimpleHash {

        private int cap;

        private int seed;

        private SimpleHash(int cap, int seed) {
            this.cap = cap;
            this.seed = seed;
        }

        public int hash(String value) {
            int result = 0;
            for (int i = 0; i < value.length(); i++) {
                result = seed * result + value.charAt(i);
            }
            return (cap - 1) & result;
        }
    }

    public static void main(String[] args) {

        MyBloomFilter bloomFilter = new MyBloomFilter();
        bloomFilter.add("123");
        System.out.println(bloomFilter.contains("123"));
        System.out.println(bloomFilter.contains("456"));
    }
}
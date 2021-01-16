package com;

import java.util.*;

/**
 * @Author: xinyuzang
 * @Desc 一致性hash算法
 * @Date: 2020/11/25 10:55
 */
public class ConsitentHash {

    private static SortedMap<Integer, String> hashMapping = new TreeMap<>();

    /**
     * 每一个物理节点对应的虚拟节点的数目
     */
    private static final Integer virtualNode = 5;

    /**
     *  服务机器ip
     */
    private static List<String> serverIps = new ArrayList<>(Arrays.asList("192.168.10.01", "192.168.10.02", "192.168.10.03"));


    static {
        // hash->服务器ip映射
        for (String serverIp : serverIps) {
            // 机器节点
            hashMapping.put(serverIp.hashCode(), serverIp);
            for(int i = 1; i <= virtualNode; i++) {
                int hashValue = (serverIp + "#" + virtualNode).hashCode();
                // 虚拟节点
                hashMapping.put(hashValue, serverIp);
            }
        }
    }

    // 通过hash算法，将点都达到int的范围-2^31 - 2^31-1
    // 根据访问的客户端ip，使用一致性hash算法分配到不同的客户端

    private String getServerNode(String clientIp) {

        // 返回大于clientIp.hashCode的集合
        SortedMap<Integer, String> integerStringSortedMap = hashMapping.tailMap(clientIp.hashCode());
        if (integerStringSortedMap.isEmpty()) {
            return hashMapping.get(hashMapping.firstKey());
        } else {
            return hashMapping.get(integerStringSortedMap.firstKey());
        }
    }


}

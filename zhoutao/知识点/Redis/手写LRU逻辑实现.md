#### 手写LRU
*具体方法见MyLRU*  

LUR:最近最少未被使用，利用LinkedHashMap的特性
    
**源码**   
1. put：  
    1. 每当添加一个元素时，先判断一下该元素存不存在
    2. 如果不存在，则新增一个节点，并且放到链表的最后
    3. 如果存在，看一下accessOrder参数是否开启，如果开启了，则将该元素前后节点连接并且自己移动到最后
```

1.【HashMap】 putVal：如果之前没有该节点，创建节点
p.next = newNode(hash, key, value, null);
2.【LinkedHashMap】newNode：通过hash找到槽位，并且在链表上找到自己位置时创建新节点
 Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
}
3.【LinkedHashMap】linkNodeLast：如果创建新节点放置当前节点到链表尾部
 private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
}
4.【HashMap】如果之前有该节点，调整节点在链表中位置
 if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
}
5.【LinkedHashMap】afterNodeAccess：这段代码做了两件事情，其他只是一些异常的容错处理1.把当前节点的前后两个节点连起来 2.把当前节点放到最后面
void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMap.Entry<K,V> last;
        // 如果accessOrder是true并且当前节点不是尾节点，需要处理
        if (accessOrder && (last = tail) != e) { 
        // LinkedHashMap.Entry继承了HashMap的Node，存储了before和after这两个Entry，相当于每一个内部类Entry，都有头尾两个Entry，每一个Entry又有HashMap里面的Node静态内部类的功能
        // b:当前节点的前置节点 a:当前节点的后置节点
            LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            // 把当前节点的后置指针设置为空
            p.after = null;
            if (b == null)
                head = a;
            else
            // 前置节点的后置指针设置为a，也就是当前节点之前的后置节点，将这两个连起来
                b.after = a;
            if (a != null)
            // 后置节点的前置指针设置为b，也就是当前节点之前的前置节点，将这两个连起来
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
            // 将当前节点的前置指针指向尾节点
                p.before = last;
            // 尾节点的后置指针指向当前节点
                last.after = p;
            }
            // 将尾节点改为当前节点，此时p已经成为了最后一个节点，并且a,b相连
            tail = p;
            // 用在fail-fast机制（修改的时候不能插入）
            ++modCount;
        }
}
modeCount源码解释
 /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
6.【HashMap】putVal:
afterNodeInsertion(evict);
如果有新元素插入在插入之后校验
6.【LinkedHashMap】afterNodeInsertion：removeEldestEntry默认返回false，可以自己实现removeEldestEntry方法，用size设置最大值
  void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
}
```
2. get：当开启了accessOrder字段设置为true时,将当前节点信息往后挪
```
1.【LinkedHashMap】get:和hashMap一样先定位再取数，getNode就是hashMap的方法
 public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
}
2. 【LinkedHashMap】afterNodeAccess 
和上述的4一样，进行链表移动
```

***
**实现LRU代码**
```
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

```
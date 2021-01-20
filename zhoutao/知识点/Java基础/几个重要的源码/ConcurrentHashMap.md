### ConcurrentHashMap源码解析
我们看一个源码，先抓主干看，从使用去看，一般一个Map结构，无非就new的构造方法，然后get，put方法

#### 一、构造方法
```
public ConcurrentHashMap(int initialCapacity) {
    // 不允许创建容量为负数的 
    if (initialCapacity < 0)
        throw new IllegalArgumentException();
        // 如果超出容量就用默认最大容量
    int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
               MAXIMUM_CAPACITY :
               // 否则扩容
               tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
    this.sizeCtl = cap;
}
```
1. 看源码抓核心，直接就看tableSizeFor方法，通过参数可以发现扩容的倍数为1.5倍+1
2. 扩容方法：带你一行一行看
```
private static final int tableSizeFor(int c) {
    int n = c - 1;
    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```
- 首先```int n = c - 1```：表示将当前值-1，具体为什么要-1，先看后面代码
- 然后通过位运算，将减了1之后的n进行位运算；可以用替代法，假如我们的值为8(1000)，，将当前与当前值右移的值进行或运算，即1000|0100，变成1100，然后将1100与0011进行或运算，变成1111，可见这个位运算其实就是将当前的位全填充为1
```
n |= n >>> 1; 
n |= n >>> 2;
n |= n >>> 4;
n |= n >>> 8;
n |= n >>> 16;
```
- 最后一行，主要看n+1,其他都是一些极限条件的过滤。上述已经讲了是得到当前位的最大值，+1就意味着越位，升级了，之前是1111，现在就变成了10000，即9
```
(n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
```
- 回过头看第一行为什么要```int n = c - 1```，首先我们一些列的位运算方法是获得当前的位的最大值+1，即满足n<=结果 - 1 => n + 1 <= 结果, 那么对于初始n来说，它比当前写的n大了1，就可以得出传进来的n <= 结果，就可得出结论：该函数为了获取大于等于n的一个结果，这个结果满足两个条件，一个是2的整数倍的条件，第二个是大于自己的最接近的值

- 此时我们看扩容的时候，传入的是1.5倍+1；那么就可以得出结论，上述位运算是得到>=1.5倍+1的值，因为+1才能有等号描述，那么得到的值肯定>1.5倍传入的值

3. **扩容方法总结：将sizeCtl设置为>传入的值的1.5倍的容量**

#### 二、put方法

```
public V put(K key, V value) {
    return putVal(key, value, false);
}

/** Implementation for put and putIfAbsent */
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
                break;                   // no lock when adding to empty bin
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                       value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

1. putVal一大堆代码，先梳理一下主干
- 如果table是空的，调用initTable()初始化
- 如果在当前tab中，根据当前hash值没有定位到，调用casTabAt，看方法名称，大概是CAS去添加节点
- 如果hash值不为空，看hash值是不是MOVED，如果是MOVED，调用helpTransfer(tab, f)发给发，看方法名称是帮助转换
- 最后就是当前hash值在ConcurrentHashMap中找得到，synchronized找到的那个节点，下面方法和HashMap的一样，就是判断红黑树逻辑和在链表上找节点逻辑

2. initTable()
```
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)
            Thread.yield(); // lost initialization race; just spin
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```
- 很复杂，先看判断条件，假如当前sizeCtl<0，就挂起当前线程，否则CAS成功，就对table赋值初始的Node数组，数组大小为前面初始的容量
- 然后sc变成了3/4的sc，最后给了sizeCtl,sizeCtl就类似一个阈值，和HashMap的阈值类似

3. casTabAt()
```
static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                    Node<K,V> c, Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}
```
- CAS创建节点
4. 

#### 三、get方法

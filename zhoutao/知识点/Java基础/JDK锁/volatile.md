### volatile关键字

#### 可见性

JMM：与物理CPU一一对应，工作内存——线程私有，主内存——公有，读先从工作内存。

1. read：主内存->工作内存
2. load：存储工作内存变量副本
3. use：工作内存->执行引擎
4. assign：执行引擎->工作内存
5. store：工作内存->主内存
6. write：存储主内存
7. lock：主内存加锁
8. unlock

缓存一致性协议，MESI，当某个线程写了（M），因为独占的（E），需要传播把其他的线程（S）都设置为无效（I）

##### happens-before 原则：可以用来判断当先线程是否安全

1.程序次序规则：一个线程内，按照代码顺序，书写在前面的操作先行发生于书写在后面的操作；

2.锁定规则：一个unLock操作先行发生于后面对同一个锁的lock操作；

3.volatile变量规则：对一个变量的写操作先行发生于后面对这个变量的读操作；

4.传递规则：如果操作A先行发生于操作B，而操作B又先行发生于操作C，则可以得出操作A先行发生于操作C；

5.线程启动规则：Thread对象的start()方法先行发生于此线程的每个一个动作；

6.线程中断规则：对线程interrupt()方法的调用先行发生于被中断线程的代码检测到中断事件的发生；

7.线程终结规则：线程中所有的操作都先行发生于线程的终止检测，我们可以通过Thread.join()方法结束、Thread.isAlive()的返回值手段检测到线程已经终止执行；

8.对象终结规则：一个对象的初始化完成先行发生于他的finalize()方法的开始；
#### 防止指令重排序
编译器，CPU会对指令进行优化，在单线程下不会有问题，但是在多线程下会有问题。利用x86处理器下，只有storeLoad屏障

##### as-if-serial原则 不管怎么重排序单线程下程序的结果肯定和预期的一样
***
原子类
- AtomicInteger 
- AtomicLong 
- AtomicBoolean

引用类
- AtomicReference
- AtomicStampedReference——时间戳，记录多次修改
- AtomicMarkableReference——标记，记录一次修改

更新
- AtomicLongUpdater
- AtomicIntegerUpdater
- AtomicReferenceUpdater

数组
- AtomicIntegerArray
- AtomicLongArray
- AtomicReferenceArray
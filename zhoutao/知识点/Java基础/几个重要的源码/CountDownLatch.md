##### 简要说明
1. CountDownLatch是基于AQS的共享模式
2. 核心方法:countDown(),await(),构造方法
    1. countDown()：当前线程占用一个state，如果state是0，执行释放操作，唤醒主线程
    2. await():封装主线程为SHARED节点，找到节点的前一个安全节点，挂起当前主线程
    3. 构造方法：设置state计数器的数目
3. state和waitStatus的区别
    1. state是CountDownLatch的计数器
    2. waitStatus是AQS里面的同步队列里面的节点的状态
```
static final class Node {
    /** Marker to indicate a node is waiting in shared mode */
    static final Node SHARED = new Node();
    /** Marker to indicate a node is waiting in exclusive mode */
    static final Node EXCLUSIVE = null;
    /** waitStatus value to indicate thread has cancelled */
    static final int CANCELLED =  1; // 当前节点取消调度
    /** waitStatus value to indicate successor's thread needs unparking */
    static final int SIGNAL    = -1; // 后继节点等待当前节点唤醒，当一个后继节点入队，会将前继节点状态改为SIGNAL
    /** waitStatus value to indicate thread is waiting on condition */
    static final int CONDITION = -2; // 节点等待在condition上，如果调用condition的signal方法后，会将节点从等待队列转移到同步队列
    /**
     * waitStatus value to indicate the next acquireShared should
     * unconditionally propagate
     */
    static final int PROPAGATE = -3; // 共享模式下，可传播
    // ...
}
```
##### countDown()方法源码分析
1. countDown()方法
```
public void countDown() {
   sync.releaseShared(1);
}
```
2. AQS的共享释放

doReleaseShared()方法是AQS本身实现的，tryReleaseShared(arg)由CountDownLatch进行实现
```
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        doReleaseShared();
        return true;
    }
    return false;
}
```

3. CountDownLatch重写tryReleaseShared方法

只有当CAS成功并且当前是最后一个资源的时候才开始释放
```
protected boolean tryReleaseShared(int releases) {
    // Decrement count; signal when transition to zero
    for (;;) {
        int c = getState();
        if (c == 0)
            return false;
        int nextc = c-1;
        if (compareAndSetState(c, nextc))
            return nextc == 0; // 如果cas成功，并且当前是最后一个，开始释放
    }
}
```
4. AQS的doReleaseShared方法
    1. 如果当前节点是signal类型，cas替换为初始状态，如果成功，唤醒下一个安全节点；如果不成功继续cas
    2. 如果当前节点是初始化状态，cas替换为propagate状态，如果成功，继续往下，如果当前节点仍旧是头节点，说明释放成功，否则是其他线程修改了头节点信息
```
private void doReleaseShared() {
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) { // 如果节点是SIGNAL类型的并且节点CAS替换成0成功，需要唤醒下一个节点；CAS失败，继续循环尝试CAS
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) // 如果当前节点状态是初始化并且替换当前节点为PROPAGATE失败
                continue;                // loop on failed CAS
        }
        if (h == head)                   // loop if head changed // cas成功后如果h还是头节点，就结束
            break;
    }
}

private void unparkSuccessor(Node node) {
    /*
     * If status is negative (i.e., possibly needing signal) try
     * to clear in anticipation of signalling.  It is OK if this
     * fails or if status is changed by waiting thread.
     */
    int ws = node.waitStatus;
    if (ws < 0) // 如果当前节点可以
        compareAndSetWaitStatus(node, ws, 0); // 把当前节点置为初始化
    /*
     * Thread to unpark is held in successor, which is normally
     * just the next node.  But if cancelled or apparently null,
     * traverse backwards from tail to find the actual
     * non-cancelled successor.
     */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) { // 如果s为空或者被弃用
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev) // 从后往前找到第一个未被弃用的节点
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread); // 唤醒s节点
}
```

总结：CountDownLatch的countDown()方法；  
1. 通过判断当前的state的值是否为0来判断当前是否释放共享锁成功
2. 如果释放共享锁成功，调用AQS实现的释放方法   
3. AQS的释放方法doReleaseShared()首先判断是否头节点是SIGNAL节点或者是已唤醒状态(waitStatus=0),如果两者CAS都失败了，就重新尝试，成功则判断头节点是否还是当前节点，如果不是，则说明其他线程占用了锁，当前线程也有可能获取到该锁，继续循环。
4. 如果是SIGNAL节点，则唤醒下一个节点；如果是已唤醒状态，则修改状态为PROPAGATE

#####  await()方法源码分析

1. await()方法
```
public void await() throws InterruptedException {
    this.sync.acquireSharedInterruptibly(1);
}
```

2. AQS的acquireSharedInterruptibly方法

如果当前state不为0，说明有state个数的线程没有countDown()，调用doAcquireSharedInterruptibly方法
```
public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
```
3. CountDownLatch实现的tryAcquireShared方法

判断当前state计数是否为0
```
protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
}
```
4. AQS的doAcquireSharedInterruptibly方法

将主线程构造一个SHARED节点，并且找到安全点，然后挂起主线程，等待唤醒
```
private void doAcquireSharedInterruptibly(int arg)
    throws InterruptedException {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt()) // 找到安全点并挂起
                throw new InterruptedException();
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```
5. AQS的shouldParkAfterFailedAcquire方法

- 如果前驱节点是SIGNAL节点，没问题；
- 如果前驱节点是废弃节点，往前找，找到最近的waitStatus < 0 的节点
- 如果是其他<0的waitStatus，CAS替换为SIGNAL节点
```
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        /*
         * This node has already set status asking a release
         * to signal it, so it can safely park.
         */
        return true;
    if (ws > 0) {
        /*
         * Predecessor was cancelled. Skip over predecessors and
         * indicate retry.
         */
        do {
            node.prev = pred = pred.prev; // 找到非1的前驱节点
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        /*
         * waitStatus must be 0 or PROPAGATE.  Indicate that we
         * need a signal, but don't park yet.  Caller will need to
         * retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL); // CAS修改为SIGNAL
    }
    return false;
}
```
6. AQS的parkAndCheckInterrupt方法

挂起当前线程，Thread.interrupted()方法返回线程中断状态，如果已经是中断直接抛出异常,调用await方法的线程不允许被中断
```
private final boolean parkAndCheckInterrupt() {
    LockSupport.park(this); // 挂起当前线程
    return Thread.interrupted(); // 判断当前线程是否被中断并且擦除
}
```
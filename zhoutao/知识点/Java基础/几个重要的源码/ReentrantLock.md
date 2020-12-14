本文分析各种工具类和AQS是如何结合的

### 一、ReentrantLock
ReentrantLock是基于AQS的**独占模式**实现的一种锁机制，是底层的api，内部有三个静态内部类，Sync，FairSync，UnFairSync，通过复写tryAcquire方法，定义了获取锁资源的条件。

什么是独占模式？上上篇文章有说到：当一个线程抢占到某个资源之后，只有释放该资源，其他线程才能进行抢占。
#### 如何利用AQS？
1. 使用AQS独占模式，加锁lock用acquire(1),解锁unlock用release(1)
2. state：0表示没有资源被抢占，1表是当前线程抢占到了，n：表示某个线程重入了n次
3. 重写tryAcquire()和tryRelease()方法，指定只有一个线程才可以进行可重入式的抢占
#### 公平锁和非公平锁
##### 什么是公平锁和非公平锁？
1. 首先都是基于AQS的独占模式实现的锁
2. 都是可重入的，当前已经抢占到的线程可以多次加锁，解锁
3. 公平锁：所有线程都必须加入到阻塞队列中去抢占；非公平锁：谁先抢到就归谁
4. 公平锁和非公平锁区别核心代码
```
1. 公平锁有这一个条件 if (!hasQueuedPredecessors() && compareAndSetState(0, acquires))表示当前抢占到的线程必须添加到阻塞队列里面，才可以有去CAS获取资源的资格；非公平模式则没有，其他代码一摸一样
public final boolean hasQueuedPredecessors() {
    // The correctness of this depends on head being initialized
    // before tail and on head.next being accurate if the current
    // thread is first in queue.
    Node t = tail; // Read fields in reverse initialization order
    Node h = head;
    Node s;
    return h != t && // 至少有两个节点并且后继节点不是当前线程/后继节点是空？
        ((s = h.next) == null || s.thread != Thread.currentThread());
}
2. 非公平锁假设当前锁资源没有被任何线程占有，直接CAS去尝试替换state
final void lock() {
    if (compareAndSetState(0, 1)) // 先CAS一下，替换state
        setExclusiveOwnerThread(Thread.currentThread());
    else
        acquire(1);
}
```
5. 通过构造方法，使用构造方法确定是公平还是非公平，默认非公平
```
无参构造
public ReentrantLock() {
    sync = new NonfairSync();
}
带参构造
public ReentrantLock(boolean fair) {
    sync = fair ? new FairSync() : new NonfairSync();
}
```
#### 如何使用？
1. 同一时间内，只有一个线程在被跑，只有一个线程被unlock，另一个才能去抢占
2. 代码编写
```
ReentrantLock lock = new ReentrantLock();
lock.lock();
lock.unlock();
``` 
***
### 二、CountDownLatch(计数器)
CountDownLatch又称计数器,主线程等待子线程执行完毕再执行。使用AQS的共享模式API实现的，内部有一个静态内部类Sync

当主线程调用await方法时候，如果资源还未被占用完毕，即线程中只要有一个没有执行到countDown，就会阻塞主线程，当执行完毕通过共享模式的传播会唤醒被阻塞的主线程
什么是共享模式？上篇文章有说到，共享模式就是当一个线程获取到资源之后，如果下一个节点也是share节点，通过传播唤醒其他线程继续抢占其他可获取的资源

#### 如何利用AQS？
1. 使用AQS的共享模式，加锁使用await使用acquireSharedInterruptibly(1)：共享模式中断就终止，解锁countDown使用releaseShared(1)
2. state:初始构造的时候传入，n表示还有n个资源可以获取，0表示没有资源可以获取
3. 重写tryAcquireShared()和tryReleaseShared()方法，可以由多个线程同时抢占，并且当资源都抢占完毕就释放

#### 如何使用？
1. 多个线程同时抢占资源，并且主线程等待多个线程执行完毕，才执行
2. 代码编写
```
CountDownLatch latch = new CountDownLatch(8);
for(int i = 1; i <= 8; i++) {
    new Thread(new Runnable(){
        @Override
        public void run() {
            // 每个子线程业务逻辑
            latch.countDown();
        }
    }).start();
}
latch.await();
// 主线程业务逻辑
```
***
### 三、CyclicBarrier(栅栏)
CyclicBarrier又称栅栏，让一批线程同时执行，利用ReentrantLock的默认非公平锁，即AQS的独占模式+Condition实现，每一个线程都使用一个ReentrantLock的lock保证每次只有一个线程进入，然后让他们从阻塞队列移动到条件队列

调用构造方法之后，每一个线程调用await，如果资源还未被用完，都会使用Condition的await方法挂起到条件队列中，一旦用完就会使用signalAll方法唤醒所有条件队列中的线程，让他们一起执行

#### 如何利用AQS？
1. 利用ReentrantLock的非公平锁实现线程的顺序 + AQS的另一个内部类Condition控制线程与线程之间的交互
2. state：利用ReentrantLock，0表示没有资源被抢占，1表是当前线程抢占到了，n：表示某个线程重入了n次

#### 如何使用？
1. 将每一个线程调用await方法，抢占后放到条件队列中阻塞，当最后一个抢占到ReentrantLock这个锁的时候，唤醒所有await的线程
2. 代码编写
```
CyclicBarrier barrier = new CyclicBarrier(5);
for(int i = 1; i <= 5; i++) {
    new Thread(new Runnable(){
        @Override
        public void run() {
            barrier.await();
            // 业务代码
        }   
    }).start();
}
```
***
### 四、Semaphore(信号量)
Semaphore又称信号量，即当线程数目达到一定数目的时候，让他们一起去执行，也有公平和非公平两种模式，使用了AQS的共享模式来实现

不详细分析了

#### 如何利用AQS？
1. 使用AQS的共享模式
2. state，用完就重置

#### 如何使用？
1. 多个线程同时抢占资源，一组一组去执行
2. 代码编写
```
Semaphore semaphore = new Semaphore(2);
for(int i = 1; i <= 5; i++) {
    new Thread(new Runnable(){
        @Override
        public void run() {
            semaphore.acquire();
            // 业务代码
            semaphore.release();
        }
    }).start()
}
```

### 总结
1. 一个线程用AQS的独占，多个就用AQS的共享
2. 要让线程一起执行就用Condition，先把线程从阻塞队列全挪到条件队列中，然后再放回到阻塞队列尾部重新抢占
3. 让主线程后执行不用Condition，让主线程也阻塞，等待唤醒

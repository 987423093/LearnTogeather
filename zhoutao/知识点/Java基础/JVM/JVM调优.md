### 模块一、OOM实战
OOM分为方法区OOM、堆区OOM、栈区OOM

#### 一、方法区OOM——类的元信息（反射，字节码增强）
1. OOM代码：利用CGLIB
```
public class MetaspaceOverFlowTest {

    public static void main(String[] args) {

        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(MetaspaceOverFlowTest.class);
            enhancer.setUseCache(false);
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                    return methodProxy.invoke(o, args);
                }
            });
            enhancer.create();
        }

    }
}
```

2. GC调优参数
```
-XX:+PrintGCDetails 
-XX:MetaspaceSize=20m :最小元空间 
-XX:MaxMetaspaceSize=20m：最大元空间

调优原则：1.预留20%以上空间 2.元空间最大、最小设置成一样大
```

3. 出现GC之后的报错
- MiniGC
```
[GC (Metadata GC Threshold) [PSYoungGen: 9151K->320K(31744K)] 17715K->9188K(117760K), 0.0011472 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
```
1. PSYongGen():Parallel Scavenge 新生代收集器
2. 9151K->320K(31744K)：代表新生代堆区从9151K变成320K，总新生代堆区占用31744K
3. 17715K->9188K(117760K)：代表总堆区从17715K变成9188K，总堆区占用117760K
4. 0.0011472 secs：垃圾回收时间
- FullGC
```
[Full GC (Metadata GC Threshold) [PSYoungGen: 64K->0K(26624K)] [ParOldGen: 4756K->4751K(109056K)] 4820K->4751K(135680K), [Metaspace: 20022K->20022K(1069056K)], 0.0202492 secs] [Times: user=0.13 sys=0.00, real=0.02 secs]
```
1. PSYoungGen：Parallel Scavenge新生代收集器；ParOldGen：Parallel Old老年代收集器


#### 二、堆区OOM——对象创建

1. OOM代码：利用无限创建对象
```
public class HeadOverFlowTest {
    public static void main(String[] args) {
        List<HeadOverFlowTest> headOverFlowTests = new ArrayList<>();
        for (;;) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            headOverFlowTests.add(new HeadOverFlowTest());
        }
    }
}
```
2. GC参数 
```
-XX:+PrintGCDetails
-Xms10m：初始堆区
-Xmx10m：最大堆区空间
-XX:+HeapDumpOnOutOfMemoryError：报错导出堆文件
 
调优原则：1.预留30%以上空间 2.最大，最小设置成一样

-Xmn2g：年轻代2G
-XX:SurvivorRatio=4:eden区和survivor区的比值为4:1:1；默认是8
-XX:NewRatio=4：年轻代和老年代的比例为1:4;默认是2

```

3. 运行一段时间之后的报错日志

    ![堆OOM](../../img/jvm/堆oom.png)

4. 用VisualVM查看堆信息——载入

    ![堆OOM](../../img/jvm/vm_堆oom.png)

5. 用VisualVM查看新生代老年代——应用信息

    ![堆OOM](../../img/jvm/堆新生代、老年代.png)

#### 栈区OOM——方法递归调用

1. OOM代码：利用虚拟机栈（栈帧大小有限制）

```
public class StackOverFlowTest {

    private int a = 1;

    public static void main(String[] args) {

        StackOverFlowTest stackOverFlowTest = new StackOverFlowTest();

        try {
            stackOverFlowTest.test();
        } catch (Throwable e) {
            System.out.println(stackOverFlowTest.a);
            e.printStackTrace();
        }
    }

    private void test() {
        a++;
        test();
    }
}
```
2. GC调优参数

```
-XX:+PrintGCDetails
-Xss156k :最大栈大小156K
```

3. 出现GC之后的报错
     
     ![栈OOM](../../img/jvm/栈oom.png)
     
(4)总结：
1. 调优的主要目的一个是为了减少Full GC，因为频繁的Full GC会导致程序挂掉；而减少Full GC那就要根据我们使用的内存情况分配合理的堆内存，经过我的验证大概一个简单对象30W个大概占用100M，所以如果一个Java程序如果使用了很多内存缓存的话，建议将堆内存调整的大一点，不然如果回收不了就会OOM，抛出异常导致我们程序终止。
2. 堆调优——如果在一个方法内部，创建对象很多，可以调整堆空间大小：-Xmx?(最大堆区)/-Xms?(初始堆区)
3. 方法区调优——如果频繁用到动态代理，可以调整元空间大小：-XX:MetaspaceSize=?(初始元空间大小设置)或者-XX：MaxMetaspaceSize=?(最大元空间大小设置)
4. 栈区调优——如果一个方法内部递归太深入，调整栈帧大小：-Xss256k(一个方法栈能够占用的空间，这个参数如果内部无任何逻辑能够递归1742次，尽量递归不要太深)

(5)疑问：
启动了堆区的例子，然后用visualVM观察发现eden区：survivor from:survivor to = 3 : 1 : 1；yong:old=1：2；新生代比例和理论的8:1:1不符啊，不知道为什么？  
 <img src="../../img/jvm/堆区占比疑问.png">
 <img src="../../img/jvm/jvm参数.png">
    
***

### 模块二、常见问题排查

#### 一、CPU占用过高

1. 首先用top命令查看占用CPU最高的进程
2. 用top -H -p pid 查看占用CPU最高的线程
3. 使用jstack命令定位线程信息
```
jstack pid | grep (线程id十进制转十六进制) -C 30(查看前后30行，-A：后，-B：前)
```
【其实用工具也可以看到，VisualVM/JProfile】
##### 其他命令
1. jps -l:查看Linux中的Java进程（jps命令是读取/tmp/xxx 下面的文件，如果进程意外关闭，并不会删除，从而导致没有进程，但是仍旧有jps信息存在）

***
### 模块三、垃圾收集器总结
垃圾收集器之前的一些摘抄,忘了出自哪的了

#### 1.Serial收集器
Serial收集器是最基本、历史最久的收集器，曾是新生代收集唯一选择，是单线程，只会使用一个CPU或者一条收集线程去完成垃圾收集工作，并且在它收集时，必须暂停其他所有工作线程，直到结束。是虚拟机运行在client模式下默认的新生代收集器：简单高效，没有线程切换的开销  
<img src="../../img/jvm/Serial.png">
#### 2.ParNew收集器
ParNew收集器是Serial收集器的多线程版本，除了多线程，收集算法、stop the world、对象分配规则、回收策略等)同Serial收集器一样。   
是许多运行在Server模式下的JVM中首选的新生代收集器，其中另一个原因是除了Serial外，只有他能和老年代CMS收集器配合工作  
 <img src="../../img/jvm/ParNew.png">
#### 3. Parallel Scavenge收集器
 
新生代收集器，并行的多线程收集器，它的目标是达到一个可控的吞吐量(CPU运行用户代码的时间和CPU总消耗时间的比值 吞吐量 =  运行用户代码的时间 /(运行用户代码的时间 + 垃圾收集的时间), 可以高效率的利用CPU时间，尽快完成程序的运算任务，适合在后台运行而不需要太多交互的任务
#### 4. Serial Old收集器
 
Serial收集器的老年代版本，单线程，标记整理算法，主要给Client模式下的虚拟机使用  
在Sever模式下 JDK1.5之前可以和Parallel Scavenge收集器搭配使用  
作为CMS后备方案，当CMS发生Concurrent Mode Failure时使用  
 <img src="../../img/jvm/SerialOld.png">
#### 5. Parallel Old收集器
Parallel Scavenge的老年代版本，多线程，标记整理算法，JDK1.6之后出现。此前Parallel Scavenge只能和Serial Old搭配使用，由于后者性能差
 
在吞吐量和CPU敏感的场合，可以使用Parallel Scavenge/Parallel Old组合  
 <img src="../../img/jvm/ParallelOld.png">
#### 6. CMS收集器
CMS(Concurrent Mark Sweep)收集器是一种以获取最短回收停顿为目标的收集器，停顿时间短，基于“标记清除”算法，并发收集、低停顿
 
 - 初始标记：仅仅标记GC Roots能直接关联到的对象，速度快，但是需要Stop the World
 - 并发标记：进行追踪引用链的过程，可以和用户线程并发执行，耗时最长
 - 重新标记：修正并发标记阶段因用户线程继续运行而导致标记发生变化的那部分对象的标记记录，比初始标记时间长但比并发标记时间短，需要Stop the World
 - 并发清除：清除标记为可以回收对象，可以和用户线程并发执行
 
整个过程耗时最长的并发标记和并发清除都可以和用户线程一起工作，从总体上来看,CMS收集器的内存回收过程和用户线程是并发执行的

缺点：
 - 对CPU资源非常敏感
 - 并发收集虽然不会暂停用户线程，但因为占用一部分CPU资源，还是会导致应用程序变慢，总吞吐量降低。  
 - CMS的默认收集线程数目 = （CPU数目+3）/4；当CPU数量多余4个，收集线程占用的CPU资源多于25%，影响大；不足4个，影响更大
 - 无法处理浮动垃圾（在并发清除时，用户线程新产生的垃圾，可能出现Concurrent Mode Failure失败）Full GC  
 - 并发清除时需要预留一定内存空间，不能像其他收集器在老年代几乎填满再进行收集；如果预留内存空间会出现Concurrent Mode Failure失败；此时JVM启动后备方案启用Serial Old收集器，导致一次Full GC产生  
 - 产生大量内存碎片：CMS基于标记-清除算法，清楚后不进行压缩操作产生大量不连续的内存碎片，当对象较大会触发Full GC
 
 <img src="../../img/jvm/CMS.png">

#### 7. G1收集器
G1(Garbage-First)是JDK-U4才正式推出商用的收集器。G1是面向服务端应用的垃圾收集器。它的使命是未来可以替换掉CMS收集器
 
特性：
  - 并行和并发：能充分利用多CPU、多核环境的硬件又是，缩短停顿时间，能和用户线程并发执行。
  - 分代收集：G1可以不需要和其它GC收集器配合就能地理管理整个堆，采用不同的方式处理新生对象和已经存活一段时间的对象
  - 空间整合：整体上看采用标记整理算法，局部看采用复制算法，两个Region之间，不会有内存碎片，优于CMS收集器
  - 可预测的停顿：可以建立可以预测停顿的时间模型，能够让使用者明确指定再一个长度为Mms的时间片段内，消耗再垃圾收集上的时间不超过Nms
  
**为什么能做到可预测?**  
- 因为可以有计划避免再整个Java堆中进行全区域的垃圾收集
- G1收集器将内存大小分为两个相等的独立区域(Region)，新生代和老年代概念保留，不再物理隔离
- G1跟踪各个Region获得其收集价值大小，在后台维护一个优先列表
- 每次根据允许的收集时间，优先回收价值最大的Region(Garbage-First名称由来)
 
**对象被其他Region的对象引用了怎么办?**
- 分代收集器都有这个问题,JVM使用Remembered Set来避免全局扫描
- 每个Region都有一个对应的Remembered Set
- 每次Reference类型数据写操作时，都会产生一个Write Barrier暂时中断操作
- 然后检查将要写入的引用指向的对象是否和该Reference类型数据再不同的Region(其他收集器：检查老年代对象是否引用了新生代对象）
- 如果不同，通过CardTable把将官引用信息记录到引用只想对象所在的Region对应的Remembered Set中
- 进行垃圾收集时，再GC根节点的枚举范围加入Remembered Set,就可以保证不进行全局扫描，也不会有遗漏
 
**比计算维护Remembered Set的操作，回收过程可以分为4个步骤(类似CMS)**
- 初始标记：仅仅标记GC Roots能直接关联到的对象，并修改TAMS(Next Top at Mark Start)的值，让下一阶段用户程序并发运行时能再正确可用的Region中创建新对象，需要Stop the world
- 并发标记：从GC Roots开始进行可达性分析，找出存活时间长，耗时长，可与用户线程并发执行
- 最终标记：修正并发表及阶段因用户线程继续运行而导致标记发生变化的那部分对象的标记记录。并发标记时虚拟机将对象变化记录在线程Remember Set Logs里面，最终标记阶段将Remember Set LoGS整合到Remember Set 中，比初始标记时间长但远比标记时间段，需要Stop the world
- 筛选回收：首先对各个Region的回收价值和成本进行排序，然根据用户期望的GC停顿时间来定制回收计划，最后按计划回收一些价值高的Region中垃圾对象。回收时采用复制算法，从一个或多个Region复制存活对象到堆上另一个空的Region，并且在此过程中压缩和释放内存；可以并发进行，降低停顿时间，并且增加吞吐量
 
<img src="../../img/jvm/G1.png">
 
 
**JVM中各种垃圾收集器的特点**  
<img src="../../img/jvm/综合垃圾回收器.png">
 
 
### 模块四、深入理解JVM参数
1. 首先可以使用java -XX:+PrintFlagsInitial pid来打印当前运行的java程序的JVM参数
2. 看详细的一个JVM进程参数
```
jps 先随便挑一个java进程
java -XX:+PrintFlagsInitial 23176
[Global flags]
     intx ActiveProcessorCount                      = -1                                  {product}
    uintx AdaptiveSizeDecrementScaleFactor          = 4                                   {product}
    uintx AdaptiveSizeMajorGCDecayTimeScale         = 10                                  {product}
    uintx AdaptiveSizePausePolicy                   = 0                                   {product}
    uintx AdaptiveSizePolicyCollectionCostMargin    = 50                                  {product}
    uintx AdaptiveSizePolicyInitializingSteps       = 20                                  {product}
    uintx AdaptiveSizePolicyOutputInterval          = 0                                   {product}
    uintx AdaptiveSizePolicyWeight                  = 10                                  {product}
    uintx AdaptiveSizeThroughPutPolicy              = 0                                   {product}
    uintx AdaptiveTimeWeight                        = 25                                  {product}
     bool AdjustConcurrency                         = false                               {product}
     bool AggressiveHeap                            = false                               {product}
     bool AggressiveOpts                            = false                               {product}
     intx AliasLevel                                = 3                                   {C2 product}
     bool AlignVector                               = true                                {C2 product}
     intx AllocateInstancePrefetchLines             = 1                                   {product}
     intx AllocatePrefetchDistance                  = -1                                  {product}
     intx AllocatePrefetchInstr                     = 0                                   {product}
     intx AllocatePrefetchLines                     = 3                                   {product}
     intx AllocatePrefetchStepSize                  = 16                                  {product}
     intx AllocatePrefetchStyle                     = 1                                   {product}
     bool AllowJNIEnvProxy                          = false                               {product}
     bool AllowNonVirtualCalls                      = false                               {product}
     bool AllowParallelDefineClass                  = false                               {product}
     bool AllowUserSignalHandlers                   = false                               {product}
     bool AlwaysActAsServerClassMachine             = false                               {product}
     bool AlwaysCompileLoopMethods                  = false                               {product}
     bool AlwaysLockClassLoader                     = false                               {product}
     bool AlwaysPreTouch                            = false                               {product}
     bool AlwaysRestoreFPU                          = false                               {product}
     bool AlwaysTenure                              = false                               {product}
     bool AssertOnSuspendWaitFailure                = false                               {product}
     bool AssumeMP                                  = false                               {product}
     intx AutoBoxCacheMax                           = 128                                 {C2 product}
    uintx AutoGCSelectPauseMillis                   = 5000                                {product}
     intx BCEATraceLevel                            = 0                                   {product}
     intx BackEdgeThreshold                         = 100000                              {pd product}
     bool BackgroundCompilation                     = true                                {pd product}
    uintx BaseFootPrintEstimate                     = 268435456                           {product}
     intx BiasedLockingBulkRebiasThreshold          = 20                                  {product}
     intx BiasedLockingBulkRevokeThreshold          = 40                                  {product}
     intx BiasedLockingDecayTime                    = 25000                               {product}
     intx BiasedLockingStartupDelay                 = 4000                                {product}
     bool BindGCTaskThreadsToCPUs                   = false                               {product}
     bool BlockLayoutByFrequency                    = true                                {C2 product}
     intx BlockLayoutMinDiamondPercentage           = 20                                  {C2 product}
     bool BlockLayoutRotateLoops                    = true                                {C2 product}
     bool BranchOnRegister                          = false                               {C2 product}
     bool BytecodeVerificationLocal                 = false                               {product}
     bool BytecodeVerificationRemote                = true                                {product}
     bool C1OptimizeVirtualCallProfiling            = true                                {C1 product}
     bool C1ProfileBranches                         = true                                {C1 product}
     bool C1ProfileCalls                            = true                                {C1 product}
     bool C1ProfileCheckcasts                       = true                                {C1 product}
     bool C1ProfileInlinedCalls                     = true                                {C1 product}
     bool C1ProfileVirtualCalls                     = true                                {C1 product}
     bool C1UpdateMethodData                        = true                                {C1 product}
     intx CICompilerCount                           = 2                                   {product}
     bool CICompilerCountPerCPU                     = false                               {product}
     bool CITime                                    = false                               {product}
     bool CMSAbortSemantics                         = false                               {product}
    uintx CMSAbortablePrecleanMinWorkPerIteration   = 100                                 {product}
     intx CMSAbortablePrecleanWaitMillis            = 100                                 {manageable}
    uintx CMSBitMapYieldQuantum                     = 10485760                            {product}
    uintx CMSBootstrapOccupancy                     = 50                                  {product}
     bool CMSClassUnloadingEnabled                  = true                                {product}
    uintx CMSClassUnloadingMaxInterval              = 0                                   {product}
     bool CMSCleanOnEnter                           = true                                {product}
     bool CMSCompactWhenClearAllSoftRefs            = true                                {product}
    uintx CMSConcMarkMultiple                       = 32                                  {product}
     bool CMSConcurrentMTEnabled                    = true                                {product}
    uintx CMSCoordinatorYieldSleepCount             = 10                                  {product}
     bool CMSDumpAtPromotionFailure                 = false                               {product}
     bool CMSEdenChunksRecordAlways                 = true                                {product}
    uintx CMSExpAvgFactor                           = 50                                  {product}
     bool CMSExtrapolateSweep                       = false                               {product}
    uintx CMSFullGCsBeforeCompaction                = 0                                   {product}
    uintx CMSIncrementalDutyCycle                   = 10                                  {product}
    uintx CMSIncrementalDutyCycleMin                = 0                                   {product}
     bool CMSIncrementalMode                        = false                               {product}
    uintx CMSIncrementalOffset                      = 0                                   {product}
     bool CMSIncrementalPacing                      = true                                {product}
    uintx CMSIncrementalSafetyFactor                = 10                                  {product}
    uintx CMSIndexedFreeListReplenish               = 4                                   {product}
     intx CMSInitiatingOccupancyFraction            = -1                                  {product}
    uintx CMSIsTooFullPercentage                    = 98                                  {product}
   double CMSLargeCoalSurplusPercent                = 0.950000                            {product}
   double CMSLargeSplitSurplusPercent               = 1.000000                            {product}
     bool CMSLoopWarn                               = false                               {product}
    uintx CMSMaxAbortablePrecleanLoops              = 0                                   {product}
     intx CMSMaxAbortablePrecleanTime               = 5000                                {product}
    uintx CMSOldPLABMax                             = 1024                                {product}
    uintx CMSOldPLABMin                             = 16                                  {product}
    uintx CMSOldPLABNumRefills                      = 4                                   {product}
    uintx CMSOldPLABReactivityFactor                = 2                                   {product}
     bool CMSOldPLABResizeQuicker                   = false                               {product}
    uintx CMSOldPLABToleranceFactor                 = 4                                   {product}
     bool CMSPLABRecordAlways                       = true                                {product}
    uintx CMSParPromoteBlocksToClaim                = 16                                  {product}
     bool CMSParallelInitialMarkEnabled             = true                                {product}
     bool CMSParallelRemarkEnabled                  = true                                {product}
     bool CMSParallelSurvivorRemarkEnabled          = true                                {product}
    uintx CMSPrecleanDenominator                    = 3                                   {product}
    uintx CMSPrecleanIter                           = 3                                   {product}
    uintx CMSPrecleanNumerator                      = 2                                   {product}
     bool CMSPrecleanRefLists1                      = true                                {product}
     bool CMSPrecleanRefLists2                      = false                               {product}
     bool CMSPrecleanSurvivors1                     = false                               {product}
     bool CMSPrecleanSurvivors2                     = true                                {product}
    uintx CMSPrecleanThreshold                      = 1000                                {product}
     bool CMSPrecleaningEnabled                     = true                                {product}
     bool CMSPrintChunksInDump                      = false                               {product}
     bool CMSPrintEdenSurvivorChunks                = false                               {product}
     bool CMSPrintObjectsInDump                     = false                               {product}
    uintx CMSRemarkVerifyVariant                    = 1                                   {product}
     bool CMSReplenishIntermediate                  = true                                {product}
    uintx CMSRescanMultiple                         = 32                                  {product}
    uintx CMSSamplingGrain                          = 16384                               {product}
     bool CMSScavengeBeforeRemark                   = false                               {product}
    uintx CMSScheduleRemarkEdenPenetration          = 50                                  {product}
    uintx CMSScheduleRemarkEdenSizeThreshold        = 2097152                             {product}
    uintx CMSScheduleRemarkSamplingRatio            = 5                                   {product}
   double CMSSmallCoalSurplusPercent                = 1.050000                            {product}
   double CMSSmallSplitSurplusPercent               = 1.100000                            {product}
     bool CMSSplitIndexedFreeListBlocks             = true                                {product}
     intx CMSTriggerInterval                        = -1                                  {manageable}
    uintx CMSTriggerRatio                           = 80                                  {product}
     intx CMSWaitDuration                           = 2000                                {manageable}
    uintx CMSWorkQueueDrainThreshold                = 10                                  {product}
     bool CMSYield                                  = true                                {product}
    uintx CMSYieldSleepCount                        = 0                                   {product}
    uintx CMSYoungGenPerWorker                      = 67108864                            {pd product}
    uintx CMS_FLSPadding                            = 1                                   {product}
    uintx CMS_FLSWeight                             = 75                                  {product}
    uintx CMS_SweepPadding                          = 1                                   {product}
    uintx CMS_SweepTimerThresholdMillis             = 10                                  {product}
    uintx CMS_SweepWeight                           = 75                                  {product}
     bool CheckEndorsedAndExtDirs                   = false                               {product}
     bool CheckJNICalls                             = false                               {product}
     bool ClassUnloading                            = true                                {product}
     bool ClassUnloadingWithConcurrentMark          = true                                {product}
     intx ClearFPUAtPark                            = 0                                   {product}
     bool ClipInlining                              = true                                {product}
    uintx CodeCacheExpansionSize                    = 65536                               {pd product}
    uintx CodeCacheMinimumFreeSpace                 = 512000                              {product}
     bool CollectGen0First                          = false                               {product}
     bool CompactFields                             = true                                {product}
     intx CompilationPolicyChoice                   = 0                                   {product}
ccstrlist CompileCommand                            =                                     {product}
    ccstr CompileCommandFile                        =                                     {product}
ccstrlist CompileOnly                               =                                     {product}
     intx CompileThreshold                          = 10000                               {pd product}
     bool CompilerThreadHintNoPreempt               = true                                {product}
     intx CompilerThreadPriority                    = -1                                  {product}
     intx CompilerThreadStackSize                   = 0                                   {pd product}
    uintx CompressedClassSpaceSize                  = 1073741824                          {product}
    uintx ConcGCThreads                             = 0                                   {product}
     intx ConditionalMoveLimit                      = 3                                   {C2 pd product}
     intx ContendedPaddingWidth                     = 128                                 {product}
     bool ConvertSleepToYield                       = true                                {pd product}
     bool ConvertYieldToSleep                       = false                               {product}
     bool CrashOnOutOfMemoryError                   = false                               {product}
     bool CreateMinidumpOnCrash                     = false                               {product}
     bool CriticalJNINatives                        = true                                {product}
     bool DTraceAllocProbes                         = false                               {product}
     bool DTraceMethodProbes                        = false                               {product}
     bool DTraceMonitorProbes                       = false                               {product}
     bool Debugging                                 = false                               {product}
    uintx DefaultMaxRAMFraction                     = 4                                   {product}
     intx DefaultThreadPriority                     = -1                                  {product}
     intx DeferPollingPageLoopCount                 = -1                                  {product}
     intx DeferThrSuspendLoopCount                  = 4000                                {product}
     bool DeoptimizeRandom                          = false                               {product}
     bool DisableAttachMechanism                    = false                               {product}
     bool DisableExplicitGC                         = false                               {product}
     bool DisplayVMOutputToStderr                   = false                               {product}
     bool DisplayVMOutputToStdout                   = false                               {product}
     bool DoEscapeAnalysis                          = true                                {C2 product}
     bool DontCompileHugeMethods                    = true                                {product}
     bool DontYieldALot                             = false                               {pd product}
    ccstr DumpLoadedClassList                       =                                     {product}
     bool DumpReplayDataOnError                     = true                                {product}
     bool DumpSharedSpaces                          = false                               {product}
     bool EagerXrunInit                             = false                               {product}
     intx EliminateAllocationArraySizeLimit         = 64                                  {C2 product}
     bool EliminateAllocations                      = true                                {C2 product}
     bool EliminateAutoBox                          = true                                {C2 product}
     bool EliminateLocks                            = true                                {C2 product}
     bool EliminateNestedLocks                      = true                                {C2 product}
     intx EmitSync                                  = 0                                   {product}
     bool EnableContended                           = true                                {product}
     bool EnableResourceManagementTLABCache         = true                                {product}
     bool EnableSharedLookupCache                   = true                                {product}
     bool EnableTracing                             = false                               {product}
    uintx ErgoHeapSizeLimit                         = 0                                   {product}
    ccstr ErrorFile                                 =                                     {product}
    ccstr ErrorReportServer                         =                                     {product}
   double EscapeAnalysisTimeout                     = 20.000000                           {C2 product}
     bool EstimateArgEscape                         = true                                {product}
     bool ExitOnOutOfMemoryError                    = false                               {product}
     bool ExplicitGCInvokesConcurrent               = false                               {product}
     bool ExplicitGCInvokesConcurrentAndUnloadsClasses  = false                               {product}
     bool ExtendedDTraceProbes                      = false                               {product}
    ccstr ExtraSharedClassListFile                  =                                     {product}
     bool FLSAlwaysCoalesceLarge                    = false                               {product}
    uintx FLSCoalescePolicy                         = 2                                   {product}
   double FLSLargestBlockCoalesceProximity          = 0.990000                            {product}
     bool FailOverToOldVerifier                     = true                                {product}
     bool FastTLABRefill                            = true                                {product}
     intx FenceInstruction                          = 0                                   {ARCH product}
     intx FieldsAllocationStyle                     = 1                                   {product}
     bool FilterSpuriousWakeups                     = true                                {product}
    ccstr FlightRecorderOptions                     =                                     {product}
     bool ForceNUMA                                 = false                               {product}
     bool ForceTimeHighResolution                   = false                               {product}
     intx FreqInlineSize                            = 325                                 {pd product}
   double G1ConcMarkStepDurationMillis              = 10.000000                           {product}
    uintx G1ConcRSHotCardLimit                      = 4                                   {product}
    uintx G1ConcRSLogCacheSize                      = 10                                  {product}
     intx G1ConcRefinementGreenZone                 = 0                                   {product}
     intx G1ConcRefinementRedZone                   = 0                                   {product}
     intx G1ConcRefinementServiceIntervalMillis     = 300                                 {product}
    uintx G1ConcRefinementThreads                   = 0                                   {product}
     intx G1ConcRefinementThresholdStep             = 0                                   {product}
     intx G1ConcRefinementYellowZone                = 0                                   {product}
    uintx G1ConfidencePercent                       = 50                                  {product}
    uintx G1HeapRegionSize                          = 0                                   {product}
    uintx G1HeapWastePercent                        = 5                                   {product}
    uintx G1MixedGCCountTarget                      = 8                                   {product}
     intx G1RSetRegionEntries                       = 0                                   {product}
    uintx G1RSetScanBlockSize                       = 64                                  {product}
     intx G1RSetSparseRegionEntries                 = 0                                   {product}
     intx G1RSetUpdatingPauseTimePercent            = 10                                  {product}
     intx G1RefProcDrainInterval                    = 10                                  {product}
    uintx G1ReservePercent                          = 10                                  {product}
    uintx G1SATBBufferEnqueueingThresholdPercent    = 60                                  {product}
     intx G1SATBBufferSize                          = 1024                                {product}
     intx G1UpdateBufferSize                        = 256                                 {product}
     bool G1UseAdaptiveConcRefinement               = true                                {product}
    uintx GCDrainStackTargetSize                    = 64                                  {product}
    uintx GCHeapFreeLimit                           = 2                                   {product}
    uintx GCLockerEdenExpansionPercent              = 5                                   {product}
     bool GCLockerInvokesConcurrent                 = false                               {product}
    uintx GCLogFileSize                             = 8192                                {product}
    uintx GCPauseIntervalMillis                     = 0                                   {product}
    uintx GCTaskTimeStampEntries                    = 200                                 {product}
    uintx GCTimeLimit                               = 98                                  {product}
    uintx GCTimeRatio                               = 99                                  {product}
    uintx HeapBaseMinAddress                        = 2147483648                          {pd product}
     bool HeapDumpAfterFullGC                       = false                               {manageable}
     bool HeapDumpBeforeFullGC                      = false                               {manageable}
     bool HeapDumpOnOutOfMemoryError                = false                               {manageable}
    ccstr HeapDumpPath                              =                                     {manageable}
    uintx HeapFirstMaximumCompactionCount           = 3                                   {product}
    uintx HeapMaximumCompactionInterval             = 20                                  {product}
    uintx HeapSizePerGCThread                       = 87241520                            {product}
     bool IgnoreEmptyClassPaths                     = false                               {product}
     bool IgnoreUnrecognizedVMOptions               = false                               {product}
    uintx IncreaseFirstTierCompileThresholdAt       = 50                                  {product}
     bool IncrementalInline                         = true                                {C2 product}
    uintx InitialBootClassLoaderMetaspaceSize       = 4194304                             {product}
    uintx InitialCodeCacheSize                      = 2555904                             {pd product}
    uintx InitialHeapSize                           = 0                                   {product}
    uintx InitialRAMFraction                        = 64                                  {product}
   double InitialRAMPercentage                      = 1.562500                            {product}
    uintx InitialSurvivorRatio                      = 8                                   {product}
    uintx InitialTenuringThreshold                  = 7                                   {product}
    uintx InitiatingHeapOccupancyPercent            = 45                                  {product}
     bool Inline                                    = true                                {product}
    ccstr InlineDataFile                            =                                     {product}
     intx InlineSmallCode                           = 1000                                {pd product}
     bool InlineSynchronizedMethods                 = true                                {C1 product}
     bool InsertMemBarAfterArraycopy                = true                                {C2 product}
     intx InteriorEntryAlignment                    = 16                                  {C2 pd product}
     intx InterpreterProfilePercentage              = 33                                  {product}
     bool JNIDetachReleasesMonitors                 = true                                {product}
     bool JavaMonitorsInStackTrace                  = true                                {product}
     intx JavaPriority10_To_OSPriority              = -1                                  {product}
     intx JavaPriority1_To_OSPriority               = -1                                  {product}
     intx JavaPriority2_To_OSPriority               = -1                                  {product}
     intx JavaPriority3_To_OSPriority               = -1                                  {product}
     intx JavaPriority4_To_OSPriority               = -1                                  {product}
     intx JavaPriority5_To_OSPriority               = -1                                  {product}
     intx JavaPriority6_To_OSPriority               = -1                                  {product}
     intx JavaPriority7_To_OSPriority               = -1                                  {product}
     intx JavaPriority8_To_OSPriority               = -1                                  {product}
     intx JavaPriority9_To_OSPriority               = -1                                  {product}
     bool LIRFillDelaySlots                         = false                               {C1 pd product}
    uintx LargePageHeapSizeThreshold                = 134217728                           {product}
    uintx LargePageSizeInBytes                      = 0                                   {product}
     bool LazyBootClassLoader                       = true                                {product}
     intx LiveNodeCountInliningCutoff               = 40000                               {C2 product}
     bool LogCommercialFeatures                     = false                               {product}
     intx LoopMaxUnroll                             = 16                                  {C2 product}
     intx LoopOptsCount                             = 43                                  {C2 product}
     intx LoopUnrollLimit                           = 60                                  {C2 pd product}
     intx LoopUnrollMin                             = 4                                   {C2 product}
     bool LoopUnswitching                           = true                                {C2 product}
     bool ManagementServer                          = false                               {product}
    uintx MarkStackSize                             = 4194304                             {product}
    uintx MarkStackSizeMax                          = 536870912                           {product}
    uintx MarkSweepAlwaysCompactCount               = 4                                   {product}
    uintx MarkSweepDeadRatio                        = 5                                   {product}
     intx MaxBCEAEstimateLevel                      = 5                                   {product}
     intx MaxBCEAEstimateSize                       = 150                                 {product}
    uintx MaxDirectMemorySize                       = 0                                   {product}
     bool MaxFDLimit                                = true                                {product}
    uintx MaxGCMinorPauseMillis                     = 4294967295                          {product}
    uintx MaxGCPauseMillis                          = 4294967295                          {product}
    uintx MaxHeapFreeRatio                          = 70                                  {manageable}
    uintx MaxHeapSize                               = 130862280                           {product}
     intx MaxInlineLevel                            = 9                                   {product}
     intx MaxInlineSize                             = 35                                  {product}
     intx MaxJNILocalCapacity                       = 65536                               {product}
     intx MaxJavaStackTraceDepth                    = 1024                                {product}
     intx MaxJumpTableSize                          = 65000                               {C2 product}
     intx MaxJumpTableSparseness                    = 5                                   {C2 product}
     intx MaxLabelRootDepth                         = 1100                                {C2 product}
     intx MaxLoopPad                                = 15                                  {C2 product}
    uintx MaxMetaspaceExpansion                     = 5452592                             {product}
    uintx MaxMetaspaceFreeRatio                     = 70                                  {product}
    uintx MaxMetaspaceSize                          = 4294967295                          {product}
    uintx MaxNewSize                                = 4294967295                          {product}
     intx MaxNodeLimit                              = 80000                               {C2 product}
 uint64_t MaxRAM                                    = 0                                   {pd product}
    uintx MaxRAMFraction                            = 4                                   {product}
   double MaxRAMPercentage                          = 25.000000                           {product}
     intx MaxRecursiveInlineLevel                   = 1                                   {product}
    uintx MaxTenuringThreshold                      = 15                                  {product}
     intx MaxTrivialSize                            = 6                                   {product}
     intx MaxVectorSize                             = 32                                  {C2 product}
    uintx MetaspaceSize                             = 21810376                            {pd product}
     bool MethodFlushing                            = true                                {product}
    uintx MinHeapDeltaBytes                         = 170392                              {product}
    uintx MinHeapFreeRatio                          = 40                                  {manageable}
     intx MinInliningThreshold                      = 250                                 {product}
     intx MinJumpTableSize                          = 10                                  {C2 pd product}
    uintx MinMetaspaceExpansion                     = 340784                              {product}
    uintx MinMetaspaceFreeRatio                     = 40                                  {product}
    uintx MinRAMFraction                            = 2                                   {product}
   double MinRAMPercentage                          = 50.000000                           {product}
    uintx MinSurvivorRatio                          = 3                                   {product}
    uintx MinTLABSize                               = 2048                                {product}
     intx MonitorBound                              = 0                                   {product}
     bool MonitorInUseLists                         = false                               {product}
     intx MultiArrayExpandLimit                     = 6                                   {C2 product}
     bool MustCallLoadClassInternal                 = false                               {product}
    uintx NUMAChunkResizeWeight                     = 20                                  {product}
    uintx NUMAInterleaveGranularity                 = 2097152                             {product}
    uintx NUMAPageScanRate                          = 256                                 {product}
    uintx NUMASpaceResizeRate                       = 1073741824                          {product}
     bool NUMAStats                                 = false                               {product}
    ccstr NativeMemoryTracking                      = off                                 {product}
     bool NeedsDeoptSuspend                         = false                               {pd product}
     bool NeverActAsServerClassMachine              = false                               {pd product}
     bool NeverTenure                               = false                               {product}
    uintx NewRatio                                  = 2                                   {product}
    uintx NewSize                                   = 1363144                             {product}
    uintx NewSizeThreadIncrease                     = 5320                                {pd product}
     intx NmethodSweepActivity                      = 10                                  {product}
     intx NmethodSweepCheckInterval                 = 5                                   {product}
     intx NmethodSweepFraction                      = 16                                  {product}
     intx NodeLimitFudgeFactor                      = 2000                                {C2 product}
    uintx NumberOfGCLogFiles                        = 0                                   {product}
     intx NumberOfLoopInstrToAlign                  = 4                                   {C2 product}
     intx ObjectAlignmentInBytes                    = 8                                   {lp64_product}
    uintx OldPLABSize                               = 1024                                {product}
    uintx OldPLABWeight                             = 50                                  {product}
    uintx OldSize                                   = 5452592                             {product}
     bool OmitStackTraceInFastThrow                 = true                                {product}
ccstrlist OnError                                   =                                     {product}
ccstrlist OnOutOfMemoryError                        =                                     {product}
     intx OnStackReplacePercentage                  = 140                                 {pd product}
     bool OptimizeFill                              = true                                {C2 product}
     bool OptimizePtrCompare                        = true                                {C2 product}
     bool OptimizeStringConcat                      = true                                {C2 product}
     bool OptoBundling                              = false                               {C2 pd product}
     intx OptoLoopAlignment                         = 16                                  {pd product}
     bool OptoScheduling                            = false                               {C2 pd product}
    uintx PLABWeight                                = 75                                  {product}
     bool PSChunkLargeArrays                        = true                                {product}
     intx ParGCArrayScanChunk                       = 50                                  {product}
    uintx ParGCDesiredObjsFromOverflowList          = 20                                  {product}
     bool ParGCTrimOverflow                         = true                                {product}
     bool ParGCUseLocalOverflow                     = false                               {product}
    uintx ParallelGCBufferWastePct                  = 10                                  {product}
    uintx ParallelGCThreads                         = 0                                   {product}
     bool ParallelGCVerbose                         = false                               {product}
    uintx ParallelOldDeadWoodLimiterMean            = 50                                  {product}
    uintx ParallelOldDeadWoodLimiterStdDev          = 80                                  {product}
     bool ParallelRefProcBalancingEnabled           = true                                {product}
     bool ParallelRefProcEnabled                    = false                               {product}
     bool PartialPeelAtUnsignedTests                = true                                {C2 product}
     bool PartialPeelLoop                           = true                                {C2 product}
     intx PartialPeelNewPhiDelta                    = 0                                   {C2 product}
    uintx PausePadding                              = 1                                   {product}
     intx PerBytecodeRecompilationCutoff            = 200                                 {product}
     intx PerBytecodeTrapLimit                      = 4                                   {product}
     intx PerMethodRecompilationCutoff              = 400                                 {product}
     intx PerMethodTrapLimit                        = 100                                 {product}
     bool PerfAllowAtExitRegistration               = false                               {product}
     bool PerfBypassFileSystemCheck                 = false                               {product}
     intx PerfDataMemorySize                        = 32768                               {product}
     intx PerfDataSamplingInterval                  = 50                                  {product}
    ccstr PerfDataSaveFile                          =                                     {product}
     bool PerfDataSaveToFile                        = false                               {product}
     bool PerfDisableSharedMem                      = false                               {product}
     intx PerfMaxStringConstLength                  = 1024                                {product}
     intx PreInflateSpin                            = 10                                  {pd product}
     bool PreferInterpreterNativeStubs              = false                               {pd product}
     intx PrefetchCopyIntervalInBytes               = -1                                  {product}
     intx PrefetchFieldsAhead                       = -1                                  {product}
     intx PrefetchScanIntervalInBytes               = -1                                  {product}
     bool PreserveAllAnnotations                    = false                               {product}
     bool PreserveFramePointer                      = false                               {pd product}
    uintx PretenureSizeThreshold                    = 0                                   {product}
     bool PrintAdaptiveSizePolicy                   = false                               {product}
     bool PrintCMSInitiationStatistics              = false                               {product}
     intx PrintCMSStatistics                        = 0                                   {product}
     bool PrintClassHistogram                       = false                               {manageable}
     bool PrintClassHistogramAfterFullGC            = false                               {manageable}
     bool PrintClassHistogramBeforeFullGC           = false                               {manageable}
     bool PrintCodeCache                            = false                               {product}
     bool PrintCodeCacheOnCompilation               = false                               {product}
     bool PrintCommandLineFlags                     = false                               {product}
     bool PrintCompilation                          = false                               {product}
     bool PrintConcurrentLocks                      = false                               {manageable}
     intx PrintFLSCensus                            = 0                                   {product}
     intx PrintFLSStatistics                        = 0                                   {product}
     bool PrintFlagsFinal                           = false                               {product}
     bool PrintFlagsInitial                         = false                               {product}
     bool PrintGC                                   = false                               {manageable}
     bool PrintGCApplicationConcurrentTime          = false                               {product}
     bool PrintGCApplicationStoppedTime             = false                               {product}
     bool PrintGCCause                              = true                                {product}
     bool PrintGCDateStamps                         = false                               {manageable}
     bool PrintGCDetails                            = false                               {manageable}
     bool PrintGCID                                 = false                               {manageable}
     bool PrintGCTaskTimeStamps                     = false                               {product}
     bool PrintGCTimeStamps                         = false                               {manageable}
     bool PrintHeapAtGC                             = false                               {product rw}
     bool PrintHeapAtGCExtended                     = false                               {product rw}
     bool PrintHeapAtSIGBREAK                       = true                                {product}
     bool PrintJNIGCStalls                          = false                               {product}
     bool PrintJNIResolving                         = false                               {product}
     bool PrintOldPLAB                              = false                               {product}
     bool PrintOopAddress                           = false                               {product}
     bool PrintPLAB                                 = false                               {product}
     bool PrintParallelOldGCPhaseTimes              = false                               {product}
     bool PrintPromotionFailure                     = false                               {product}
     bool PrintReferenceGC                          = false                               {product}
     bool PrintSafepointStatistics                  = false                               {product}
     intx PrintSafepointStatisticsCount             = 300                                 {product}
     intx PrintSafepointStatisticsTimeout           = -1                                  {product}
     bool PrintSharedArchiveAndExit                 = false                               {product}
     bool PrintSharedDictionary                     = false                               {product}
     bool PrintSharedSpaces                         = false                               {product}
     bool PrintStringDeduplicationStatistics        = false                               {product}
     bool PrintStringTableStatistics                = false                               {product}
     bool PrintTLAB                                 = false                               {product}
     bool PrintTenuringDistribution                 = false                               {product}
     bool PrintTieredEvents                         = false                               {product}
     bool PrintVMOptions                            = false                               {product}
     bool PrintVMQWaitTime                          = false                               {product}
     bool PrintWarnings                             = true                                {product}
    uintx ProcessDistributionStride                 = 4                                   {product}
     bool ProfileInterpreter                        = true                                {pd product}
     bool ProfileIntervals                          = false                               {product}
     intx ProfileIntervalsTicks                     = 100                                 {product}
     intx ProfileMaturityPercentage                 = 20                                  {product}
     bool ProfileVM                                 = false                               {product}
     bool ProfilerPrintByteCodeStatistics           = false                               {product}
     bool ProfilerRecordPC                          = false                               {product}
    uintx PromotedPadding                           = 3                                   {product}
    uintx QueuedAllocationWarningCount              = 0                                   {product}
    uintx RTMRetryCount                             = 5                                   {ARCH product}
     bool RangeCheckElimination                     = true                                {product}
     intx ReadPrefetchInstr                         = 0                                   {ARCH product}
     bool ReassociateInvariants                     = true                                {C2 product}
     bool ReduceBulkZeroing                         = true                                {C2 product}
     bool ReduceFieldZeroing                        = true                                {C2 product}
     bool ReduceInitialCardMarks                    = true                                {C2 product}
     bool ReduceSignalUsage                         = false                               {product}
     intx RefDiscoveryPolicy                        = 0                                   {product}
     bool ReflectionWrapResolutionErrors            = true                                {product}
     bool RegisterFinalizersAtInit                  = true                                {product}
     bool RelaxAccessControlCheck                   = false                               {product}
    ccstr ReplayDataFile                            =                                     {product}
     bool RequireSharedSpaces                       = false                               {product}
    uintx ReservedCodeCacheSize                     = 50331648                            {pd product}
     bool ResizeOldPLAB                             = true                                {product}
     bool ResizePLAB                                = true                                {product}
     bool ResizeTLAB                                = true                                {pd product}
     bool RestoreMXCSROnJNICalls                    = false                               {product}
     bool RestrictContended                         = true                                {product}
     bool RewriteBytecodes                          = true                                {pd product}
     bool RewriteFrequentPairs                      = true                                {pd product}
     intx SafepointPollOffset                       = 256                                 {C1 pd product}
     intx SafepointSpinBeforeYield                  = 2000                                {product}
     bool SafepointTimeout                          = false                               {product}
     intx SafepointTimeoutDelay                     = 10000                               {product}
     bool ScavengeBeforeFullGC                      = true                                {product}
     intx SelfDestructTimer                         = 0                                   {product}
    uintx SharedBaseAddress                         = 0                                   {product}
    ccstr SharedClassListFile                       =                                     {product}
    uintx SharedMiscCodeSize                        = 122880                              {product}
    uintx SharedMiscDataSize                        = 4194304                             {product}
    uintx SharedReadOnlySize                        = 16777216                            {product}
    uintx SharedReadWriteSize                       = 16777216                            {product}
     bool ShowMessageBoxOnError                     = false                               {product}
     intx SoftRefLRUPolicyMSPerMB                   = 1000                                {product}
     bool SpecialEncodeISOArray                     = true                                {C2 product}
     bool SplitIfBlocks                             = true                                {C2 product}
     intx StackRedPages                             = 1                                   {pd product}
     intx StackShadowPages                          = 6                                   {pd product}
     bool StackTraceInThrowable                     = true                                {product}
     intx StackYellowPages                          = 3                                   {pd product}
     bool StartAttachListener                       = false                               {product}
     intx StarvationMonitorInterval                 = 200                                 {product}
     bool StressLdcRewrite                          = false                               {product}
    uintx StringDeduplicationAgeThreshold           = 3                                   {product}
    uintx StringTableSize                           = 60013                               {product}
     bool SuppressFatalErrorMessage                 = false                               {product}
    uintx SurvivorPadding                           = 3                                   {product}
    uintx SurvivorRatio                             = 8                                   {product}
     intx SuspendRetryCount                         = 50                                  {product}
     intx SuspendRetryDelay                         = 5                                   {product}
     intx SyncFlags                                 = 0                                   {product}
    ccstr SyncKnobs                                 =                                     {product}
     intx SyncVerbose                               = 0                                   {product}
    uintx TLABAllocationWeight                      = 35                                  {product}
    uintx TLABRefillWasteFraction                   = 64                                  {product}
    uintx TLABSize                                  = 0                                   {product}
     bool TLABStats                                 = true                                {product}
    uintx TLABWasteIncrement                        = 4                                   {product}
    uintx TLABWasteTargetPercent                    = 1                                   {product}
    uintx TargetPLABWastePct                        = 10                                  {product}
    uintx TargetSurvivorRatio                       = 50                                  {product}
    uintx TenuredGenerationSizeIncrement            = 20                                  {product}
    uintx TenuredGenerationSizeSupplement           = 80                                  {product}
    uintx TenuredGenerationSizeSupplementDecay      = 2                                   {product}
     intx ThreadPriorityPolicy                      = 0                                   {product}
     bool ThreadPriorityVerbose                     = false                               {product}
    uintx ThreadSafetyMargin                        = 52428800                            {product}
     intx ThreadStackSize                           = 0                                   {pd product}
    uintx ThresholdTolerance                        = 10                                  {product}
     intx Tier0BackedgeNotifyFreqLog                = 10                                  {product}
     intx Tier0InvokeNotifyFreqLog                  = 7                                   {product}
     intx Tier0ProfilingStartPercentage             = 200                                 {product}
     intx Tier23InlineeNotifyFreqLog                = 20                                  {product}
     intx Tier2BackEdgeThreshold                    = 0                                   {product}
     intx Tier2BackedgeNotifyFreqLog                = 14                                  {product}
     intx Tier2CompileThreshold                     = 0                                   {product}
     intx Tier2InvokeNotifyFreqLog                  = 11                                  {product}
     intx Tier3BackEdgeThreshold                    = 60000                               {product}
     intx Tier3BackedgeNotifyFreqLog                = 13                                  {product}
     intx Tier3CompileThreshold                     = 2000                                {product}
     intx Tier3DelayOff                             = 2                                   {product}
     intx Tier3DelayOn                              = 5                                   {product}
     intx Tier3InvocationThreshold                  = 200                                 {product}
     intx Tier3InvokeNotifyFreqLog                  = 10                                  {product}
     intx Tier3LoadFeedback                         = 5                                   {product}
     intx Tier3MinInvocationThreshold               = 100                                 {product}
     intx Tier4BackEdgeThreshold                    = 40000                               {product}
     intx Tier4CompileThreshold                     = 15000                               {product}
     intx Tier4InvocationThreshold                  = 5000                                {product}
     intx Tier4LoadFeedback                         = 3                                   {product}
     intx Tier4MinInvocationThreshold               = 600                                 {product}
     bool TieredCompilation                         = true                                {pd product}
     intx TieredCompileTaskTimeout                  = 50                                  {product}
     intx TieredRateUpdateMaxTime                   = 25                                  {product}
     intx TieredRateUpdateMinTime                   = 1                                   {product}
     intx TieredStopAtLevel                         = 4                                   {product}
     bool TimeLinearScan                            = false                               {C1 product}
     bool TraceBiasedLocking                        = false                               {product}
     bool TraceClassLoading                         = false                               {product rw}
     bool TraceClassLoadingPreorder                 = false                               {product}
     bool TraceClassPaths                           = false                               {product}
     bool TraceClassResolution                      = false                               {product}
     bool TraceClassUnloading                       = false                               {product rw}
     bool TraceDynamicGCThreads                     = false                               {product}
     bool TraceGen0Time                             = false                               {product}
     bool TraceGen1Time                             = false                               {product}
    ccstr TraceJVMTI                                =                                     {product}
     bool TraceLoaderConstraints                    = false                               {product rw}
     bool TraceMetadataHumongousAllocation          = false                               {product}
     bool TraceMonitorInflation                     = false                               {product}
     bool TraceParallelOldGCTasks                   = false                               {product}
     intx TraceRedefineClasses                      = 0                                   {product}
     bool TraceSafepointCleanupTime                 = false                               {product}
     bool TraceSharedLookupCache                    = false                               {product}
     bool TraceSuspendWaitFailures                  = false                               {product}
     intx TrackedInitializationLimit                = 50                                  {C2 product}
     bool TransmitErrorReport                       = false                               {product}
     bool TrapBasedNullChecks                       = false                               {pd product}
     bool TrapBasedRangeChecks                      = false                               {C2 pd product}
     intx TypeProfileArgsLimit                      = 2                                   {product}
    uintx TypeProfileLevel                          = 111                                 {pd product}
     intx TypeProfileMajorReceiverPercent           = 90                                  {C2 product}
     intx TypeProfileParmsLimit                     = 2                                   {product}
     intx TypeProfileWidth                          = 2                                   {product}
     intx UnguardOnExecutionViolation               = 0                                   {product}
     bool UnlinkSymbolsALot                         = false                               {product}
     bool Use486InstrsOnly                          = false                               {ARCH product}
     bool UseAES                                    = false                               {product}
     bool UseAESIntrinsics                          = false                               {product}
     intx UseAVX                                    = 99                                  {ARCH product}
     bool UseAdaptiveGCBoundary                     = false                               {product}
     bool UseAdaptiveGenerationSizePolicyAtMajorCollection  = true                                {product}
     bool UseAdaptiveGenerationSizePolicyAtMinorCollection  = true                                {product}
     bool UseAdaptiveNUMAChunkSizing                = true                                {product}
     bool UseAdaptiveSizeDecayMajorGCCost           = true                                {product}
     bool UseAdaptiveSizePolicy                     = true                                {product}
     bool UseAdaptiveSizePolicyFootprintGoal        = true                                {product}
     bool UseAdaptiveSizePolicyWithSystemGC         = false                               {product}
     bool UseAddressNop                             = false                               {ARCH product}
     bool UseAltSigs                                = false                               {product}
     bool UseAutoGCSelectPolicy                     = false                               {product}
     bool UseBMI1Instructions                       = false                               {ARCH product}
     bool UseBMI2Instructions                       = false                               {ARCH product}
     bool UseBiasedLocking                          = true                                {product}
     bool UseBimorphicInlining                      = true                                {C2 product}
     bool UseBoundThreads                           = true                                {product}
     bool UseCLMUL                                  = false                               {ARCH product}
     bool UseCMSBestFit                             = true                                {product}
     bool UseCMSCollectionPassing                   = true                                {product}
     bool UseCMSCompactAtFullCollection             = true                                {product}
     bool UseCMSInitiatingOccupancyOnly             = false                               {product}
     bool UseCRC32Intrinsics                        = false                               {product}
     bool UseCodeCacheFlushing                      = true                                {product}
     bool UseCompiler                               = true                                {product}
     bool UseCompilerSafepoints                     = true                                {product}
     bool UseCompressedClassPointers                = false                               {lp64_product}
     bool UseCompressedOops                         = false                               {lp64_product}
     bool UseConcMarkSweepGC                        = false                               {product}
     bool UseCondCardMark                           = false                               {C2 product}
     bool UseCountLeadingZerosInstruction           = false                               {ARCH product}
     bool UseCountTrailingZerosInstruction          = false                               {ARCH product}
     bool UseCountedLoopSafepoints                  = false                               {C2 product}
     bool UseCounterDecay                           = true                                {product}
     bool UseDivMod                                 = true                                {C2 product}
     bool UseDynamicNumberOfGCThreads               = false                               {product}
     bool UseFPUForSpilling                         = false                               {C2 product}
     bool UseFastAccessorMethods                    = true                                {product}
     bool UseFastEmptyMethods                       = true                                {product}
     bool UseFastJNIAccessors                       = true                                {product}
     bool UseFastStosb                              = false                               {ARCH product}
     bool UseG1GC                                   = false                               {product}
     bool UseGCLogFileRotation                      = false                               {product}
     bool UseGCOverheadLimit                        = true                                {product}
     bool UseGCTaskAffinity                         = false                               {product}
     bool UseHeavyMonitors                          = false                               {product}
     bool UseInlineCaches                           = true                                {product}
     bool UseInterpreter                            = true                                {product}
     bool UseJumpTables                             = true                                {C2 product}
     bool UseLWPSynchronization                     = true                                {product}
     bool UseLargePages                             = false                               {pd product}
     bool UseLargePagesInMetaspace                  = false                               {product}
     bool UseLargePagesIndividualAllocation        := false                               {pd product}
     bool UseLockedTracing                          = false                               {product}
     bool UseLoopCounter                            = true                                {product}
     bool UseLoopInvariantCodeMotion                = true                                {C1 product}
     bool UseLoopPredicate                          = true                                {C2 product}
     bool UseMathExactIntrinsics                    = true                                {C2 product}
     bool UseMaximumCompactionOnSystemGC            = true                                {product}
     bool UseMembar                                 = false                               {pd product}
     bool UseMontgomeryMultiplyIntrinsic            = false                               {C2 product}
     bool UseMontgomerySquareIntrinsic              = false                               {C2 product}
     bool UseMulAddIntrinsic                        = false                               {C2 product}
     bool UseMultiplyToLenIntrinsic                 = false                               {C2 product}
     bool UseNUMA                                   = false                               {product}
     bool UseNUMAInterleaving                       = false                               {product}
     bool UseNewLongLShift                          = false                               {ARCH product}
     bool UseOSErrorReporting                       = false                               {pd product}
     bool UseOldInlining                            = true                                {C2 product}
     bool UseOnStackReplacement                     = true                                {pd product}
     bool UseOnlyInlinedBimorphic                   = true                                {C2 product}
     bool UseOptoBiasInlining                       = true                                {C2 product}
     bool UsePSAdaptiveSurvivorSizePolicy           = true                                {product}
     bool UseParNewGC                               = false                               {product}
     bool UseParallelGC                             = false                               {product}
     bool UseParallelOldGC                          = false                               {product}
     bool UsePerfData                               = true                                {product}
     bool UsePopCountInstruction                    = false                               {product}
     bool UseRDPCForConstantTableBase               = false                               {C2 product}
     bool UseRTMDeopt                               = false                               {ARCH product}
     bool UseRTMLocking                             = false                               {ARCH product}
     bool UseSHA                                    = false                               {product}
     bool UseSHA1Intrinsics                         = false                               {product}
     bool UseSHA256Intrinsics                       = false                               {product}
     bool UseSHA512Intrinsics                       = false                               {product}
     intx UseSSE                                    = 99                                  {product}
     bool UseSSE42Intrinsics                        = false                               {product}
     bool UseSerialGC                               = false                               {product}
     bool UseSharedSpaces                           = true                                {product}
     bool UseSignalChaining                         = true                                {product}
     bool UseSquareToLenIntrinsic                   = false                               {C2 product}
     bool UseStoreImmI16                            = true                                {ARCH product}
     bool UseStringDeduplication                    = false                               {product}
     bool UseSuperWord                              = true                                {C2 product}
     bool UseTLAB                                   = true                                {pd product}
     bool UseThreadPriorities                       = true                                {pd product}
     bool UseTypeProfile                            = true                                {product}
     bool UseTypeSpeculation                        = true                                {C2 product}
     bool UseUTCFileTimestamp                       = true                                {product}
     bool UseUnalignedLoadStores                    = false                               {ARCH product}
     bool UseVMInterruptibleIO                      = false                               {product}
     bool UseXMMForArrayCopy                        = false                               {product}
     bool UseXmmI2D                                 = false                               {ARCH product}
     bool UseXmmI2F                                 = false                               {ARCH product}
     bool UseXmmLoadAndClearUpper                   = true                                {ARCH product}
     bool UseXmmRegToRegMoveAll                     = false                               {ARCH product}
     bool VMThreadHintNoPreempt                     = false                               {product}
     intx VMThreadPriority                          = -1                                  {product}
     intx VMThreadStackSize                         = 0                                   {pd product}
     intx ValueMapInitialSize                       = 11                                  {C1 product}
     intx ValueMapMaxLoopSize                       = 8                                   {C1 product}
     intx ValueSearchLimit                          = 1000                                {C2 product}
     bool VerifyMergedCPBytecodes                   = true                                {product}
     bool VerifySharedSpaces                        = false                               {product}
     intx WorkAroundNPTLTimedWaitHang               = 1                                   {product}
    uintx YoungGenerationSizeIncrement              = 20                                  {product}
    uintx YoungGenerationSizeSupplement             = 80                                  {product}
    uintx YoungGenerationSizeSupplementDecay        = 8                                   {product}
    uintx YoungPLABSize                             = 4096                                {product}
     bool ZeroTLAB                                  = false                               {product}
     intx hashCode                                  = 5                                   {product}

```
3. 看官方的JVM信息手册
[url](https://www.oracle.com/java/technologies/javase/vmoptions-jsp.html)
[url](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/java.html)

4. 常用的JVM参数
```
-Xms:堆初始值 默认1/64内存，1024的整数倍
-Xmx:堆最大值 默认1/4内存，1024的整数倍
-Xmn:新生代最大值
-XX:MetaspaceSize/PermSize:元空间初始值/持久代
-XX:MaxMetaspaceSize/MaxPermSize:元空间最大值/持久代最大值  默认20M
-Xss:线程栈最大值 默认512K
-XX:SurvivorRatio:Eden区和Survivor的比值 默认8，8:1:1
-XX:NewRatio:新生代和老年代的比值 默认2，2:1
一般开启是+，关闭是-
-XX:+UseSerialGC:新生代使用Serial收集器，单线程
-XX:+UseParNewGC:新生代使用ParNew收集器，Serial的多线程版本
-XX:+UseParallelGC:新生代使用Parallel Scavenge收集器，注重吞吐量
-XX:+UseParallelOldGC:老年代使用Parallel Old收集器，Parallel Scavenge收集器的老年代版本
-XX:+UseConcMarkSweepGC:老年代使用CMS收集器，尽量减少stop the world，分为四段；1.收集所有GCRoot扫到的类（stop the world） 2.和用户线程并发收集 3.对二阶段收集的内容进行校验，重新标记(stop the world) 4.进行回收
-XX:+UseG1GC:使用G1收集器，分代收集器，不区分年轻代，老年代，可预测。有Region，每一个Region都有一个Remember Set 1.标记GCRoot直接关联到的对象，修改标记值，防止并发时能够用可用的Region创建新对象(Stop the world) 2.并发标记：可达性分析找出对象，与用户线程并发 3.最终标记：修正标记，记录在线程的RememberSetLogs里面，最终整合到RememberSet里面（Stop the world） 4.对每一个Region进行价值判断，然后利用用户期望的GC停顿时间进行回收，Region之间使用复制算法
```
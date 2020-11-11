#### Dubbo工作原理
1. service：接口层，给服务提供者和消费者实现
2. registry：服务注册层，服务注册和发现，刚开始初始化的时候，消费者会将提供者地址缓存到本地内存，挂了可以继续通信
3. monitor：监控层，对rpc调用次数和调用时间进行监控
4. protocal：远程调用层，封装rpc调用
5. proxy：代理层，提供者和消费者都会生成代理，通过代理之间网络进行通信
6. cluster：集群层，封装多个服务提供者的路由和负载均衡，将多个实例整合成一个服务
7. transport：数据传输层，网络传输层，抽象netty和mina为统一接口
8. serialize：数据序列化层
9. exchange：信息交换层，封装请求模式，同步转异步
10. config：配置层，对dubbo进行各种配置

#### 工作流程
1. provider向注册中心注册
2. consumer从注册中心订阅服务，注册中心通知consumer注册好的服务
3. consumer调用provider
4. consumer和provider异步通知monitor

#### 通信协议以及协议对应的序列化方案
**dubbo支持hessian,java二进制序列化，json，SOAP文本序列化，hessian为默认序列化协议**
1. dubbo协议——基于hessian序列化协议
   1. 单一长连接，NIO异步通信
   2. 场景：传输数据量小（m每次请求100kb），并发量高，服务提供方少，消费方多
2. rmi协议——java二进制
   1. 多个短连接
   2. 场景：文件传输，消费者和提供者数目相近
3. hessian协议——hessian序列化
   1. 多个短连接
   2. 场景：文件传输，提供者比消费者多
4. http协议——json序列化
5. webserver——SOAP文本序列化

#### 负载均衡策略
1. random loadbalance：随机调用实现负载均衡，可以设置权重
2. roundrobin locadbalance：均匀将流量达到各个机器上，如果机器配置不一样，需要调整权重
3. leastactive loadbalance：自动感知，给不活跃的性能差的机器更少的请求
4. consistanthash loadbalance：一致性hash算法，相同参数请求发送到一个provider节点

#### 集群容错策略
1. failover cluster：失败自动切换，默认模式，常见读操作
    ```
    <dubbo:service retries="2"/>
    ```
2. failfast cluster：一次调用失败立即失败，常见非幂等性写操作
3. failsafe cluster：出现异常忽略，常见不重要的接口调用（日志）
4. failback cluster：失败了后台自动记录请求，然后定时重发，适合消息队列
5. forking cluster：并行调用多个provider，一个成功就返回，常见实时性要求比较高的，设置forks="2"设置最大并行数
6. broadcast cluster：逐个调用所有的provider，任何一个报错就报错，常见通知所有provider更新缓存/日志等本地资源信息 

#### 动态代理策略
1. 默认javassist动态字节码生成，创建代理类
2. 可以通过SPI机制配置自己的动态代理策略

#### SPI
1. 介绍：根据指定的配置或者默认的配置，找到对应的类加载进来，用这个类的实例对象
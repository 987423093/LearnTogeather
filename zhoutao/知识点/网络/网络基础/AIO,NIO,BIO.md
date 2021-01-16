1. BIO:阻塞同步IO
每次创建一个socket，都需要服务端建立一个连接，消耗资源较多

2. NIO：非阻塞同步IO
Selector：多路复用选择器；Selector.select 阻塞就绪的channel；让一个线程监视多个channel
channel：读写通道
Buffer：内存缓冲区，channel通过缓冲区进行读写

3. AIO：非阻塞异步IO
利用回调

阻塞/非阻塞：  
阻塞：类似await，等待其他服务的资源；等待客户端来消息才下去  
非阻塞：不等，先去执行；不管客户端来不来消息都执行下去

同步/异步：  
异步：异步线程回调  
同步：只有main线程


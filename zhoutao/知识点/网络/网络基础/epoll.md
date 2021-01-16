[引用](https://zhuanlan.zhihu.com/p/64138532) 

#### 一、普通建立Socket
```
//创建socket
int s = socket(AF_INET, SOCK_STREAM, 0);   
//绑定
bind(s, ...)
//监听
listen(s, ...)
//接受客户端连接
int c = accept(s, ...)
//接收客户端数据
recv(c, ...);
//将数据打印出来
printf(...)
```

#### 二、Select
为了同时监视多个socket而产生的

```
int s = socket(AF_INET, SOCK_STREAM, 0);  
bind(s, ...)
listen(s, ...)

int fds[] =  存放需要监听的socket

while(1){
    int n = select(..., fds, ...)
    for(int i=0; i < fds.count; i++){
        if(FD_ISSET(fds[i], ...)){
            //fds[i]的数据处理
        }
    }
}
```
场景：
1. 假设进程A监听三个socket，socket1,socket2,socket3；然后通过通过select让这三个socket把线程A放到socket内部的等待队列中
2. 当任何一个socket接收到数据之后，将三个socket中所有有关进程A的信息删除，中断程序唤醒线程A，放入到工作队列
3. 此时A被唤醒，A再去遍历一遍所有的socket列表，就可以找到就绪的socket。如果一个都没有就阻塞

select有缺点：
1. 每次调用select都需要将进程放入到所有监视的socket的等待队列中，默认一个select只能监视1024个socket
2. 进程被唤醒之后还要去遍历一遍，看哪些socket就绪

总结：select就是将操作放到socket里面去作处理，利用socket的等待队列

#### 三、Epoll

```
int s = socket(AF_INET, SOCK_STREAM, 0);   
bind(s, ...)
listen(s, ...)

int epfd = epoll_create(...);
epoll_ctl(epfd, ...); //将所有需要监听的socket添加到epfd中

while(1){
    int n = epoll_wait(...)
    for(接收到数据的socket){
        //处理
    }
}
```
select的缺点：
1. 每次调用select的将进程A放到所有socket的等待队列
2. 每次判断没有socket有数据，就阻塞A

epoll的做法：
不像select将操作都放在while(select)里面，现在外面创建一个epollevent的对象

#### 四、总结：
select
1. 进程A进来，先将所有socket存储一个数组
2. 使用select去将进程A放到每一个socket的等待队列中
3. 然后循环所有的socket，查看是否有socket接收到了数据
4. 如果没有socket接收到了数据，就阻塞
5. 如果有socket接收到了数据，将进程A中断，并且去删除所有socket的等待队列面带有A的信息
6. 此时进程A被唤醒，需要再去所有的socket列表看哪一个socket有内容，放到工作队列中执行


epoll
1. 进程A进来，先用epoll_create创建一个epollevent，epollevent里面会存储进程与socket的关系，以及就绪队列（类似上述socket里面的等待队列）
2. 然后使用epoll_ttl，将socket信息加入到创建的epollevent内
3. 利用epoll_wait，去遍历所有的socket看是否有socket有数据
4. 如果有socket有数据（就绪队列里面有数据），中断返回，否则阻塞
5. 此时进程A被唤醒，直接去epollevent的就绪队列上查看有哪些socket，放到工作队列中去执行

操作系统epoll
epoll_create:创建一个epollevent对象
epoll_ctl:绑定
epoll_wait:没有信息就阻塞
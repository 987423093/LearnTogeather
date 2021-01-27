### IO

**介绍BIO代码之前，先介绍对于BIO,NIO,AIO的区别**
1. BIO：同步阻塞模型
2. NIO：同步非阻塞模型
3. AIO：异步非阻塞模型

**那么什么是同步？异步？阻塞？非阻塞呢？**
1. 同步，异步：其实很好理解，一个任务用一个线程跑就是同步，一个任务用多个线程跑就是异步
2. 阻塞，非阻塞：就是阻塞当前线程，直到某个事件被触发；可以类比CountDownLatch计数器主线程的await方法；异步，就是不管你触不触发，我都往下继续执行
#### BIO代码实现

##### 一服务端一客户端
此时当客户端与服务端建立连接，会在 `int read = clientSocket.getInputStream().read(bytes);`这行代码阻塞，导致无法有多个客户端连接服务端

**服务端代码**
```
public class BioServer {

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(9000);
        System.out.println("等待连接");
        Socket clientSocket = serverSocket.accept();
        System.out.println("有客户端连接了,客户端端口" + clientSocket.getPort());
        while (true) {
            byte[] bytes = new byte[1024];
            System.out.println("等待客户端数据...");
            int read = clientSocket.getInputStream().read(bytes);
            if (read != -1) {
                System.out.println("接收到客户端数据：" + new String(bytes, 0, read));
            }
        }
    }
}
```
**客户端代码**
```
public class BioClient {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 9000);
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            socket.getOutputStream().write(message.getBytes());
            socket.getOutputStream().flush();
            System.out.println("客户端向服务端发送数据：" + message);
        }
        socket.close();
    }
}
```
1. 首先建立连接
<img src="../img/io/BIO-服务端等待连接.png"/>

2. 客户端发送数据

<img src="../img/io/BIO-客户端发送数据.png"/>

3. 服务端接收数据

<img src="../img/io/BIO-服务端接收数据.png"/>

##### 一服务端多客户端
只需要将客户端对于连接的阻塞的读操作通过线程去跑，就可以让多个客户端同事打印输出
**服务端代码**
```
public class BioServer {

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(9000);
        System.out.println("等待连接");
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("有客户端连接了,客户端端口" + clientSocket.getPort());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        byte[] bytes = new byte[1024];
                        System.out.println("等待客户端数据...");
                        int read = 0;
                        try {
                            read = clientSocket.getInputStream().read(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (read != -1) {
                            System.out.println("接收到客户端数据：" + new String(bytes, 0, read));
                        }
                    }
                }
            }).start();
        }
    }
}
```
1. 服务端使用多线程连接多个客户端

<img src="../img/io/BIO-服务端多客户端.png"/>


##### BIO带来的问题
由上述可见，由于BIO的同步阻塞性质：此时就会出现一个问题：每多一个客户端，就需要开线程来提供对应的服务端的读线程；如果客户端很多，那么对于服务端来说，消耗资源会很多；可能有人说那就用线程池呗，可是线程池只是保证了你最大可以连接的数目以及部分客户端关闭的时候那些空闲的线程能够复用，一旦你的客户端连接数目达到上万级别，那用线程池明显是不切实际的，所以就引入的NIO，也就是同步非阻塞模型，一个线程可以同时处理多个连接（Selector-IO多路复用）

下篇文章详细叙述NIO是什么
***
#### NIO代码实现

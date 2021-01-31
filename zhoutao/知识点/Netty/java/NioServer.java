import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2021/1/27 23:47
 * @Copyright: 2020 Hundsun All rights reserved.
 */
public class NioServer {

    private static List<SocketChannel> socketChannels = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(9000));
        // 设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        System.out.println("服务端等待连接");
        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                System.out.println("客户端连接成功");
                socketChannel.configureBlocking(false);
                socketChannels.add(socketChannel);
            }
            Iterator<SocketChannel> iterator = socketChannels.iterator();
            while (iterator.hasNext()) {
                SocketChannel next = iterator.next();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                long read = next.read(byteBuffer);
                if (read > 0) {
                    System.out.println("客户端地址：" + next.getRemoteAddress() + "客户端的信息：" + new String(byteBuffer.array()));
                } else if (read == -1){
                    iterator.remove();
                    System.out.println("客户端断开");
                }
            }
        }
    }
}

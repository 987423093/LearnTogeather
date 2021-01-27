import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2021/1/27 22:43
 * @Copyright: 2020 Hundsun All rights reserved.
 */
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

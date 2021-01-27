import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * @Description:
 * @Author: zhoutao29203
 * @Date: 2021/1/27 22:52
 * @Copyright: 2020 Hundsun All rights reserved.
 */
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

package j05subTCPServer;

import java.net.ServerSocket;
import java.net.Socket;

public class DelayTcpServer {

    public static void main(String[] args) throws Exception {
        long pid = ProcessHandle.current().pid();
        System.out.println("[PID] " + pid);

        try (ServerSocket server = new ServerSocket(9090)) {
            System.out.println("Delay TCP Server started: 9090");

            while (true) {
                Socket client = server.accept();
                Thread.ofPlatform().start(() -> {
                    try (client) {
                        Thread.sleep(3000);
                        client.getOutputStream().write(1);
                        client.getOutputStream().flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
}

package j02;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPerConnectionServer {

    // 생성되는 작업 스레드에 식별 번호를 부여한다.
    private static final AtomicInteger threadId = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {

        try (ServerSocket server = new ServerSocket(8080)) {

            System.out.println("Server started: 8080");

            while(true) {

                // main 스레드는 새로운 연결을 수락한다.
                Socket client = server.accept();

                // 연결마다 별도의 플랫폼 스레드는 생성한다.
                Thread worker = new Thread(() -> {
                    try {
                        handle(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, "worker-" + threadId.incrementAndGet());

                // start() 는 새 스레드에서 run()을 실행하도록 요청한다.
                // main 스레드는 worker 의 완료를 기다리지 않는다.
                worker.start();
            }
        }
    }

    private static void handle(Socket client) throws IOException, InterruptedException {

        try(client) {
            String threadName = Thread.currentThread().getName();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            client.getInputStream(),
                            StandardCharsets.US_ASCII
                    )
            );

            // 요청 데이터가 도착할 때까지 현재 worker 가 대기한다.
            String requestLine = reader.readLine();

            if (requestLine == null) {
                return ;
            }

            String[] parts = requestLine.split(" ");
            String path = parts[1];

            // HTTP 헤더가 끝날떄까지 읽는다.
            String line;
            while((line = reader.readLine()) != null && !line.isEmpty()) {
                // 이번 예제에서는 헤더를 사용하지 않는다.
            }

            if (line == null) {
                return ;
            }

            System.out.println(
                    LocalDateTime.now() +
                            " [START - " + threadName + "] " + path
            );

            switch(path) {
                case "/fast" -> send(client, "FAST");

                case "/slow" -> {
                    Thread.sleep(3000);
                    send(client, "SLOW");
                }

                default -> send(client, "NOT FOUND", 404);
            }

            System.out.println(
                    LocalDateTime.now() +
                            " [END - " + threadName + "] " + path
            );
        }
    }

    private static void send(Socket client, String body) throws IOException {
        send(client, body, 200);
    }

    private static void send(Socket client, String message, int status) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        String statusText = status == 200 ? "OK" : "Not Found";

        String header =
                "HTTP/1.1 " + status + " " + statusText + "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        OutputStream out = client.getOutputStream();

        out.write(header.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }
}

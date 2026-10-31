package j03;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolHttpServer {

    private static final AtomicInteger threadId = new AtomicInteger(0);

    private static final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    2,  // Core Pool Size
                    4,  // Maximum Pool Size
                    30, // 초과 Worker 의 유휴 유지 시간
                    TimeUnit.SECONDS,

                    // 최대 3개의 작업을 대기시킨다.
                    new ArrayBlockingQueue<>(3),

                    // Worker 스레드에 이름을 부여한다.
                    task -> new Thread(
                            task,
                            "worker-" + threadId.incrementAndGet()
                    ),

                    // 작업을 수용할 수 없으면 예외를 발생시킨다.
                    new ThreadPoolExecutor.AbortPolicy()
            );

    public static void main(String[] args) throws IOException {

        try (ServerSocket server = new ServerSocket(8080)) {

            System.out.println("Server started: 8080");

            while (true) {
                Socket client = server.accept();

                try {
                    // Socket 처리 작업을 Thread Pool 에 제출한다.
                    executor.execute(() -> {
                        try {
                            handle(client);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // 작업 제출에 실패한 Socket 을 정리한다.
                    // 이번 샘플에서는 HTTP 응답 없이 연결을 닫는다.
                    client.close();

                    System.out.println("Task rejected");
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    private static void handle(Socket client) throws IOException, InterruptedException{

        try (client) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            client.getInputStream(),
                            StandardCharsets.US_ASCII
                    )
            );

            String requestLine = reader.readLine();
            if (requestLine == null) {
                return ;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return ;
            }
            String path = parts[1];

            // HTTP 헤더의 끝까지 읽는다.
            String line;
            while((line = reader.readLine()) != null && !line.isEmpty()) {
                // 헤더는 사용하지 않는다.
            }

            if (line == null) {
                return ;
            }

            System.out.println(
                    LocalDateTime.now() +
                            " [" + Thread.currentThread().getName() + "] " +
                            path +
                            " active=" + executor.getActiveCount() +
                            " pool=" + executor.getPoolSize() +
                            " queue=" + executor.getQueue().size()
            );

            switch (path) {
                case "/fast" -> send(client, "FAST");

                case "/slow" -> {
                    Thread.sleep(3000);
                    send(client, "SLOW");
                }

                case "/status" -> {
                    String status =
                            "active=" + executor.getActiveCount() +
                                    "\npool=" + executor.getPoolSize() +
                                    "\nqueue=" + executor.getQueue().size();

                    send(client, status);
                }

                default -> send(client, "NOT FOUND" + 404);
            }
        }
    }

    private static void send(Socket client, String message) throws IOException {
        send(client, message, 200);
    }

    private static void send(Socket client, String message, int status) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        String statusText =
                status == 200 ? "OK" : "Not Found";

        String headers =
                "HTTP/1.1 " + status + " " + statusText + "\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\n" +
                        "Content-Length: " + body.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        OutputStream out = client.getOutputStream();

        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }
}

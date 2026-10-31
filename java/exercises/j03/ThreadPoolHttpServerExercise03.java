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

/**
 * J-03 연습문제
 * 제한된 Thread Pool과 과부하 처리
 *
 * Thread Pool 설정
 * Core Pool Size: 2
 * Maximum Pool Size: 4
 * Queue Capacity: 3
 * Keep Alive Time: 30초
 * Rejection Policy: AbortPolicy
 *
 * 요구사항
 * 1. J-02의 서버를 ThreadPoolExecutor 기반으로 변경합니다.
 * 2. /fast는 즉시 FAST를 반환합니다.
 * 3. /slow는 3초 대기한 뒤 SLOW를 반환합니다.
 * 4. /status는 Pool Size, Active Count, Queue Size, Completed Task Count를 반환합니다.
 * 5. 작업이 거절되면 HTTP 503 Service Unavailable을 반환합니다.
 * 6. 요청 처리 시 스레드 이름과 Pool 상태를 로그에 기록합니다.
 * 7. Worker가 처리한 Socket은 반드시 닫습니다.
 *
 * 제약조건
 * - Java 17 이상, JDK 표준 라이브러리만 사용
 * - ThreadPoolExecutor 직접 생성
 * - Executors.newFixedThreadPool() 사용 금지
 * - Queue는 ArrayBlockingQueue 사용
 * - 작업 거절 시 서버가 종료되면 안 됨
 * - 정답 코드는 직접 작성
 *
 * 완료 기준
 * - 동시 요청에서 Worker 수가 최대 4개로 제한됩니다.
 * - 대기 Queue에 최대 3개 작업이 쌓입니다.
 * - 처리 용량을 초과한 요청은 503 응답을 받습니다.
 * - 요청 처리 후 Queue가 비워지고 Worker가 재사용됩니다.
 */
public class ThreadPoolHttpServerExercise03 {

    private static final AtomicInteger threadId = new AtomicInteger(0);

    private static final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    2,
                    4,
                    30,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(3),
                    task -> new Thread(task, "worker-" + threadId.incrementAndGet()),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    public static void main(String[] args) throws IOException {
        try(ServerSocket server = new ServerSocket(8080)) {
            System.out.println("Server started: 8080");

            while(true) {
                Socket client = server.accept();

                try {
                    executor.execute(() -> {
                        try {
                            handle(client);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    try(client) {
                        send(client, "Service Unavailable", 503);
                    } catch (IOException ioException) {
                        System.err.println("Failed to send 503: " + ioException.getMessage());
                    }
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
                            "pool=" + executor.getPoolSize() +
                                    "\nactive=" + executor.getActiveCount() +
                                    "\nqueue=" + executor.getQueue().size() +
                                    "\ncompleted=" + executor.getCompletedTaskCount();

                    send(client, status);
                }

                default -> send(client, "NOT FOUND", 404);
            }
        }
    }

    private static void send(Socket client, String message) throws IOException {
        send(client, message, 200);
    }

    private static void send(Socket client, String message, int status) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        String statusText = "OK";
        switch (status) {
            case 404 -> statusText = "Not Found";
            case 503 -> statusText = "Service Unavailable";
        }

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

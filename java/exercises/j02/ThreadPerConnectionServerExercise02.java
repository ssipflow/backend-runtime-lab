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

/**
 * J-02 연습문제
 * 동시 요청 처리 및 Worker 수 추적
 * J-01에서 작성한 서버를 기반으로 Thread-per-Connection 구조를 구현하십시오.
 *
 * 요구사항
 * 1. 클라이언트 연결마다 새로운 플랫폼 스레드를 생성합니다.
 * 2. /fast는 즉시 FAST를 반환합니다.
 * 3. /slow는 3초 대기한 후 SLOW를 반환합니다.
 * 4. /status는 현재 요청 처리 중인 Worker 수를 반환합니다.
 * 5. Worker가 요청 처리를 시작하면 카운터를 증가시키고, 종료하면 감소시킵니다.
 * 6. Worker 시작 및 종료 시각, 스레드 이름, 활성 Worker 수를 로그에 기록합니다.
 *
 * 제약조건
 * - Java 17 이상
 * - new Thread() 사용
 * - ExecutorService, ThreadPoolExecutor 사용 금지
 * - 활성 Worker 수는 동시성에 안전한 방식으로 관리
 * - 예외가 발생해도 Worker 카운터가 정상적으로 감소해야 함
 *
 * 완료 기준
 * - /slow 처리 중 /fast가 지연 없이 처리됩니다.
 * - 여러 /slow 요청이 동시에 처리됩니다.
 * - /status에서 활성 Worker 수를 확인할 수 있습니다.
 * - 모든 요청이 종료된 후 Worker 카운터가 정상 상태로 복귀합니다.
 */
public class ThreadPerConnectionServerExercise02 {

    private static final AtomicInteger threadId = new AtomicInteger(0);
    private static final AtomicInteger activeWorkers = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {

        try (ServerSocket server = new ServerSocket(8080)) {

            System.out.println("Server started: 8080");

            while(true) {
                Socket client = server.accept();

                Thread worker = new Thread(() -> {
                    activeWorkers.incrementAndGet();
                    try {
                        handle(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        activeWorkers.decrementAndGet();
                    }
                }, "worker-" + threadId.incrementAndGet());

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

            String requestLine = reader.readLine();

            if (requestLine == null) {
                return ;
            }

            String[] parts = requestLine.split(" ");
            String path = parts[1];

            String line;
            while((line = reader.readLine()) != null && !line.isEmpty()) {
                // 이번 예제에서는 헤더를 사용하지 않는다.
            }

            if (line == null) {
                return ;
            }

            System.out.println(
                    LocalDateTime.now() +
                            " [START - " + threadName + "] " +
                            " [Thread Count - " + activeWorkers.get() + "] " +
                            path
            );

            switch(path) {
                case "/fast" -> send(client, "FAST");

                case "/slow" -> {
                    Thread.sleep(3000);
                    send(client, "SLOW");
                }

                case "/status" -> {
                    send(client, "CURRENT THREAD COUNT - " + activeWorkers.get());
                }

                default -> send(client, "NOT FOUND", 404);
            }

            System.out.println(
                    LocalDateTime.now() +
                            " [END - " + threadName + "] " +
                            " [Thread Count - " + activeWorkers.get() + "] " +
                            path
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

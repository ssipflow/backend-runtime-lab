package j05;

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
 * J-05 연습문제
 * CPU 작업과 Blocking I/O 비교 서버
 *
 * Thread Pool 설정은 다음과 같습니다.
 * Core Pool Size - 4
 * Maximum Pool Size - 4
 * Queue Capacity - 20
 * Rejection Policy	AbortPolicy
 *
 * 구현할 엔드포인트
 * 엔드포인트	동작
 * /cpu	약 3초 동안 CPU 계산 반복
 * /wait	Thread.sleep(3000) 후 응답
 * /io	별도 TCP 서버에 연결하여 데이터를 읽을 때까지 대기
 * /status	Pool Size, Active Count, Queue Size 출력
 *
 * 구현 요구사항
 * 1. /cpu는 Thread.sleep() 없이 반복 계산을 수행합니다.
 * 2. /cpu의 실행 시간을 약 3초로 제한합니다. System.nanoTime()으로 경과 시간을 판단할 수 있습니다.
 * 3. /wait는 Thread.sleep(3000)을 사용합니다.
 * 4. /io는 별도 TCP 서버에 접속하여 InputStream.read()를 수행합니다.
 * 5. TCP 서버는 연결 후 약 3초 뒤에 1바이트를 전송합니다.
 * 6. 각 요청의 시작 시각, 종료 시각, Worker 이름, 처리 시간을 로그로 기록합니다.
 * 7. /cpu, /wait, /io의 Thread State와 CPU 사용량을 비교합니다.
 *
 * 제약조건
 * - Java 17 이상, JDK 표준 라이브러리만 사용
 * - CPU 작업에 sleep() 사용 금지
 * - /io는 실제 Socket I/O를 사용해야 함
 * - ThreadPoolExecutor 사용
 * - Socket과 Stream은 반드시 정리
 * - 정답 코드는 직접 작성
 *
 * 완료 기준
 * - 세 작업의 응답 시간과 CPU 사용량을 비교
 * - Thread Dump에서 각 작업의 상태 확인
 * - 동시 요청 증가 시 Thread Pool의 처리 특성 설명
 */
public class CpuWaitExercise {

    private static final AtomicInteger threadId = new AtomicInteger(0);

    private static volatile long result;

    private static final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    4,
                    4,
                    30,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(20),
                    task -> new Thread(
                            task,
                            "worker-" + threadId.incrementAndGet()
                    ),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    public static void main(String[] args) throws IOException {
        System.out.println("[PID] " + ProcessHandle.current().pid());

        try (ServerSocket server = new ServerSocket(8080)) {
            while (true) {
                Socket client = server.accept();

                try {
                    executor.execute(() ->{
                        try {
                            handle(client);
                        } catch (Exception e) {
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

    private static void handle(Socket client) throws IOException, InterruptedException {
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
            String path = parts[1].toLowerCase().trim();

            long start = System.nanoTime();
            System.out.println(
                    LocalDateTime.now() +
                            " [" +
                            Thread.currentThread().getName() +
                            "] " +
                            "Started " +
                            path
            );
            switch (path) {
                case "/cpu" -> cpuTask(client);
                case "/wait" -> waitTask(client);
                case "/io" -> ioTask(client);
                case "/status" -> statusTask(client);
                default -> send(client, "NOT FOUND", 404);
            }
            long duration = System.nanoTime() - start;
            System.out.println(
                    LocalDateTime.now() +
                            " [" +
                            Thread.currentThread().getName() +
                            "] " +
                            "Done " +
                            path +
                            " Duration: " + TimeUnit.NANOSECONDS.toSeconds(duration)
            );

        }
    }

    private static void cpuTask(Socket client) throws IOException {
        long startTime = System.nanoTime();
        long duration = TimeUnit.SECONDS.toNanos(3);

        long value = 0;
        while(true) {
            if (System.nanoTime() - startTime >= duration) {
                break;
            }
            value += 1;
        }

        result = value;
        send(client, "[DONE] cpuTask result=" + result);
    }

    private static void waitTask(Socket client) throws InterruptedException, IOException {
        Thread.sleep(3000);
        send(
                client,
                "[" + Thread.currentThread().getName() + "]" +"[DONE] waitTask"
        );
    }

    private static void ioTask(Socket client) throws IOException {
        try(Socket socket = new Socket("localhost", 9090)) {
            int value = socket.getInputStream().read();
            System.out.println("[IO] Received: " + value);
        }

        send(
                client,
                "[" + Thread.currentThread().getName() + "]" +"[DONE] ioTask"
        );
    }

    private static void statusTask(Socket client) throws IOException {
        long poolSize = executor.getPoolSize();
        long activeCount = executor.getActiveCount();
        long queueSize = executor.getQueue().size();

        String status = String.format(
                "pool=%d\nactive=%d\nqueue=%d",
                poolSize,
                activeCount,
                queueSize
        );

        send(client, status);
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

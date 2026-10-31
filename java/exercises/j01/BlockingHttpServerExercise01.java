package j01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * J-01 연습문제
 * 요청 처리 시간이 다른 HTTP 서버 구현
 * 앞서 제공한 샘플(examples/BlockingHttpServer)을 참고하되, URL 경로에 따라 처리 시간이 달라지는 단일 스레드 서버를 구현하십시오.
 *
 * 요구사항
 * 1. GET /fast 요청에는 즉시 FAST를 반환합니다.
 * 2. GET /slow 요청에는 3초 대기한 뒤 SLOW를 반환합니다.
 * 3. 모든 요청은 main 스레드 하나에서 순차적으로 처리합니다.
 * 4. HTTP 상태 코드와 Content-Length를 올바르게 설정합니다.
 * 5. 요청 시작과 종료 시각, 스레드 이름을 로그에 기록합니다.
 *
 * 제약조건
 * - Java 17 이상
 * - JDK 표준 라이브러리만 사용
 * - ExecutorService, 별도 스레드 생성 금지
 * - HTTP 요청 본문 처리 불필요
 *
 * 완료 기준
 * - 두 엔드포인트가 정상 응답합니다.
 * - /slow 처리 중 /fast 요청이 들어왔을 때 동작을 관찰할 수 있습니다.
 * - 그 결과를 Blocking I/O와 단일 스레드 실행 구조로 설명할 수 있습니다.
 */
public class BlockingHttpServerExercise01 {

    public static void main(String[] args) throws IOException, InterruptedException {

        try(ServerSocket server = new ServerSocket(8080)) {
            System.out.println("Server started: 8080");

            while (true) {
                Socket client = server.accept();

                System.out.println(LocalDateTime.now() +
                        " [Connected - " + Thread.currentThread().getName() + "] " +
                        client.getRemoteSocketAddress()
                );

                handle(client);

                if (client.isClosed()) {
                    System.out.println(LocalDateTime.now() +
                            " [Closed - " + Thread.currentThread().getName() + "] " +
                            client.getRemoteSocketAddress()
                    );
                }
            }
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
            String[] requestLines = requestLine.split(" ");
            String path = requestLines[1].toLowerCase();

            System.out.println(LocalDateTime.now() +
                    " [Requested - " + Thread.currentThread().getName() + "] " +
                    path
            );

            switch (path) {
                case "/fast":
                    send(client, HttpURLConnection.HTTP_OK,  "FAST");
                    break;
                case "/slow":
                    Thread.sleep(3000);
                    send(client, HttpURLConnection.HTTP_OK, "SLOW");
                    break;
                default:
                    send(client, HttpURLConnection.HTTP_NOT_FOUND, "HTTP NOT FOUND");
                    break;
            }
        }
    }

    private static void send(Socket client, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);

        String statusCode = "200 OK";
        if (status == HttpURLConnection.HTTP_NOT_FOUND) {
            statusCode = "404 NOT FOUND";
        }

        String headers =
                "HTTP/1.1 " + statusCode + "\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\n" +
                        "Content-Length: " + body.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";

        OutputStream out = client.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }
}

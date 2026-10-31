import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class BlockingHttpServer {

    public static void main(String[] args) throws IOException {

        // TCP 8080 포트에서 연결 요청을 수신한다
        try (ServerSocket server = new ServerSocket(8080)) {
           System.out.println("Server started: 8080");

           while (true) {
               // 클라이언트 연결이 들어올 때까지 대기한다.
               // 연결이 수락되면 통신용 Socket 을 반환한다.
               Socket client = server.accept();

               System.out.println("[Connected] " + client.getRemoteSocketAddress());

               // 현재 main 스레드에서 요청을 처리한다.
               // 이 작업이 끝나기 전에는 다음 accept()를 호출하지 않는다.
               handle(client);

               if (client.isClosed()) {
                   System.out.println("[Closed] " + client.getRemoteSocketAddress());
               }
           }
        }
    }

    private static void handle(Socket client) throws IOException {

        try(client) {

            // 클라이언트가 전송한 데이터를 읽기 위한 스트림
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            client.getInputStream(),
                            StandardCharsets.US_ASCII
                    )
            );

            // HTTP 요청의 첫 번째 줄을 읽는다.
            // 데이터가 도착하지 않는다면 여기서 Blocking 된다.
            String requestLine = reader.readLine();

            if (requestLine == null) {
                return ;
            }

            System.out.println("[Requested] " + requestLine);

            // HTTP 헤더의 끝(빈 줄)까지 읽는다.
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // 이번 예제는 헤더를 해석하지 않는다.
            }

            if (line == null) {
                return ;
            }

            // HTTP 응답 본문을 구성한다.
            byte[] body = "Hello Java Server".getBytes(StandardCharsets.UTF_8);

            String headers =
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain; charset=utf-8\r\n" +
                            "Content-Length: " + body.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";

            // 응답 헤더와 본문을 클라이언트에 전송한다.
            OutputStream out = client.getOutputStream();
            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
        }
    }
}

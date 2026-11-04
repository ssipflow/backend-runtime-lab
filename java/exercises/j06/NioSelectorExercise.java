package j06;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * J-06 연습문제: NIO HTTP 서버
 *
 * 구현 클래스: NioSelectorExercise
 *
 * 서버 포트: 8080
 * 엔드포인트	동작
 * /fast	즉시 HTTP 200 응답
 * /status	현재 연결 수와 Event Loop 스레드 이름 응답
 * 기타	HTTP 404 응답
 *
 * 구현 요구사항
 * 1. ServerSocketChannel과 Selector를 사용합니다.
 * 2. 서버와 클라이언트 Channel을 Non-blocking 모드로 설정합니다.
 * 3. OP_ACCEPT, OP_READ, OP_WRITE를 처리합니다.
 * 4. 연결별 ByteBuffer를 SelectionKey.attachment()에 보관합니다.
 * 5. HTTP 요청 헤더의 끝인 \r\n\r\n까지 데이터를 누적합니다.
 * 6. HTTP 응답은 Content-Length와 Connection: close를 포함합니다.
 * 7. SocketChannel.write()가 일부 바이트만 기록할 수 있으므로 남은 데이터가 있으면 이후 OP_WRITE 이벤트에서 이어서 전송합니다.
 * 8. 응답 전송이 끝나면 Channel을 닫고 연결 수를 감소시킵니다.
 * 9. 연결 하나에서 오류가 발생해도 다른 연결의 처리는 계속되어야 합니다.
 *
 * 제약조건
 * - Java 17 이상
 * - JDK 표준 라이브러리만 사용
 * - ThreadPoolExecutor 사용 금지
 * - 요청당 스레드 생성 금지
 * - Thread.sleep() 사용 금지
 * - Selector 기반 Event Loop는 하나만 사용
 * - 각 연결의 읽기 버퍼에 최대 요청 크기 제한 적용
 * - 정답 구현은 직접 작성
 *
 * 완료 기준
 * - /fast 정상 응답
 * - /status에서 연결 수 확인
 * - 다수 연결에서도 Worker 스레드가 증가하지 않음
 * - 일부만 도착한 HTTP 요청을 처리할 수 있음
 * - 부분 쓰기를 고려한 응답 처리
 */
public class NioSelectorExercise {

    private static final int PORT = 8080;
    private static final int MAX_REQUEST_SIZE = 8192;

    private static int activeConnections = 0;

    public static void main(String[] args) throws IOException {
        try(
                Selector selector = Selector.open();
                ServerSocketChannel server = ServerSocketChannel.open();
        ) {
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(PORT));
            server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("NIO Server started: " + PORT);
            System.out.println("PID: " + ProcessHandle.current().pid());

            while(true) {
                selector.select();
                nioEventLoopHandler(selector);
            }
        }
    }

    /**
     * Selector가 감지한 이벤트를 해당 핸들러에 전달한다.
     *
     * 힌트:
     * - SelectionKey 하나에서 오류가 발생해도
     *   나머지 Key 처리는 계속되어야 한다.
     * - 유효하지 않은 Key 는 건너뛴다.
     */
    private static void nioEventLoopHandler(Selector selector) {
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

        while(iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();

            if (!key.isValid()) continue;

            try {
                if (key.isAcceptable()) {
                    httpConnAccept(selector, key);
                } else if (key.isReadable()) {
                    httpRequestRead(key);
                } else if (key.isWritable()) {
                    httpResponseWrite(key);
                }
            } catch (IOException e) {
                System.err.println("[ERROR] " + e.getMessage());
                closeConnection(key);
            }
        }
    }

    /**
     * 새로운 클라이언트 연결을 수락한다.
     */
    private static void httpConnAccept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();

        if (client == null) {
            return ;
        }

        try {
            client.configureBlocking(false);
            ConnectionState state = new ConnectionState();
            client.register(selector, SelectionKey.OP_READ, state);
            activeConnections++;

            System.out.println("[ACCEPT] " + client.getRemoteAddress() +
                    ", connections=" + activeConnections);
        } catch (IOException | RuntimeException e) {
            client.close();
            throw e;
        }
    }

    /**
     * 클라이언트의 HTTP 요청을 읽는다.
     *
     * TODO 1:
     * - readBuffer 에 수신된 데이터를 읽는다.
     * - HTTP 헤더 종료 시퀀스(\r\n\r\n)를 찾는다.
     * - 헤더가 완성되지 않았다면 OP_READ를 유지한다.
     * - 최대 요청 크기를 초과하면 연결을 종료한다.
     *
     * TODO 2:
     * - 헤더가 완성되면 HTTP요청 경로를 추출한다.
     * - 경로에 맞는 HTTP 응답을 생성한다.
     * - writeBuffer에 응답을 저장한다.
     * - interstOps 를 OP_WRITE로 변경한다.
     */
    private static void httpRequestRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        int bytesRead = client.read(state.readBuffer);
        if (bytesRead == -1) {
            closeConnection(key);
            return ;
        }

        if (bytesRead == 0) {
            return ;
        }

        // ByteBuffer 를 읽기 모드로 전환
        state.readBuffer.flip();
        while(state.readBuffer.hasRemaining()) {
            byte value = state.readBuffer.get();

            // TODO 1-1:
            // 수신한 바이트를 state.request에 누적한다.
            //
            // 힌트:
            // state.request.write(value);
            state.request.write(value);

            byte[] bytes = state.request.toByteArray();
            int length = bytes.length;

            // MAX_REQUEST_SIZE 초과 여부 확인
            if (length > MAX_REQUEST_SIZE) {
                closeConnection(key);
                return ;
            }

            // HTTP 헤더 종료 조건:
            // 마지막 4바이트가 \r\n\r\n 인지 확인
            boolean isCompleted = false;
            if (length >= 4) {
                isCompleted =
                        bytes[length - 4] == '\r' &&
                                bytes[length - 3] == '\n' &&
                                bytes[length - 2] == '\r' &&
                                bytes[length - 1] == '\n';
            }
            if (!isCompleted) continue;


            // 헤더가 완성되었다면 요청 경로 추출
            //
            // 예:
            // GET /fast HTTP/1.1
            //
            // -> "/fast"
            String request = state.request.toString(StandardCharsets.US_ASCII);
            String[] parts = request.split(" ");
            if (parts.length < 2) {
                closeConnection(key);
                return ;
            }
            String path = parts[1].trim();

            // 요청 경로에 따라 HTTP 응답 생성
            //
            // /fast    -> HTTP 200
            // /status  -> HTTP 200 + 연결 수
            // 기타       -> HTTP 404
            ByteBuffer resBuffer;
            switch (path) {
                case "/fast" -> resBuffer = createHttpResponse(200, "[FAST] OK\n");
                case "/status" -> resBuffer = createHttpResponse(200, "[STATUS] activeConnections=" + activeConnections + "\n");
                default -> resBuffer = createHttpResponse(404, "NOT FOUND\n");
            }

            // state.writeBuffer = ...
            // key.interestOps(SelectionKey.OP_WRITE);
            state.writeBuffer = resBuffer;
            key.interestOps(SelectionKey.OP_WRITE);
            return ;
        }
        // 아직 처리하지 않은 바이트를 보존한다.
        state.readBuffer.compact();
    }

    /**
     * HTTP 응답을 전송한다.
     *
     * TODO:
     * - SocketChannel.write() 호출
     * - 남은 바이트가 있으면 OP_WRITE 유지
     * - 모두 전송했다면 연결 종료
     */
    private static void httpResponseWrite(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState)  key.attachment();
        ByteBuffer buffer = state.writeBuffer;

        // client.write(buffer);
        client.write(buffer);

        // buffer.hasRemaining() 으로 전송완료 여부 확인
        //
        // 힌트:
        // 전송이 완료되지 않았다면 아무것도 변경하지 않아도
        // 다음 OP_WRITE 이벤트에서 다시 처리할 수 있다.
        if(buffer.hasRemaining()) {
            return ;
        }

        // 전송이 완료되었다면 closeConnection(key)
        closeConnection(key);
    }

    /**
     * HTTP 응답 생성
     *
     * TODO:
     * - HTTP 상태코드
     * - Content-Type
     * - Content-Length
     * - Connection: close
     * - CRLF
     *
     * 힌트:
     * Content-Length는 문자열 길이가 아니라
     * UTF-8 인코딩 이후의 바이트 길이
     */
    private static ByteBuffer createHttpResponse(int status, String body) {
        // HTTP 응답을 byte[]로 생성한 뒤
        // ByteBuffer.wrap(...)으로 변환
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        String statusText = status == 200 ? "OK" : "NOT FOUND";
        String headers =
                "HTTP/1.1 " + status + " " + statusText + "\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n";
        byte[] headerBytes = headers.getBytes(StandardCharsets.UTF_8);

        ByteBuffer response = ByteBuffer.allocate(
                headerBytes.length + bodyBytes.length
        );

        response.put(headerBytes);
        response.put(bodyBytes);
        response.flip();

        return response;
    }

    /**
     * 연결 종료를 한 곳에서 관리한다.
     *
     * 힌트:
     * - 정상 종료와 예외 종료 모두 이 메서드를 사용한다.
     * - 연결 수가 중복 감소하지 않도록 주의한다.
     */
    private static void closeConnection(SelectionKey key) {
        if (!key.isValid()) return ;

        try {
            key.channel().close();
        } catch (IOException e) {
            System.err.println("[CLOSE ERROR] " + e.getMessage());
        } finally {
            key.cancel();

            // 서버 채널은 연결 수에 포함하지 않는다.
            if (key.channel() instanceof SocketChannel) {
                activeConnections--;
            }
        }
    }

    /**
     * 클라이언트별 HTTP 요청/응답 상태
     */
    private static class ConnectionState {
        // SocketChannel 에서 읽은 바이트를 임시 보관
        final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

        // 여러 READ 이벤트에 걸쳐 수신한 HTTP 요청 누적
        ByteArrayOutputStream request = new ByteArrayOutputStream();

        // HTTP 응답 전송 위치를 유지
        ByteBuffer writeBuffer;
    }
}

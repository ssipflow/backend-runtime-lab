package j06;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class NioSelectorExample {

    public static void main(String[] args) throws IOException {

        // 여러 Channel 의 I/O 준비 상태를 감시한다.
        try(Selector selector = Selector.open()) {
            ServerSocketChannel server = ServerSocketChannel.open();

            server.bind(new InetSocketAddress(8080));
            // Selector에 등록하려면 non-blocking 모드여야 한다.
            server.configureBlocking(false);

            // 새로운 연결을 수락할 준비가 되었는지 감시한다.
            server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("NIO Server started: 8080");
            System.out.println("PID: " + ProcessHandle.current().pid());

            while (true) {
                // IO 이벤트가 발생할때까지 대기한다.
                selector.select();

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    // 처리한 이벤트를 Selected Set 에서 제거한다.
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            accept(selector, key);
                        } else if (key.isReadable()) {
                            read(key);
                        } else if (key.isWritable()) {
                            write(key);
                        }
                    } catch(IOException e) {
                        // 한 연결의 오류가 전체 Event Loop를
                        // 종료시키지 않도록 해당 연결만 정리한다.
                        System.out.println("Channel error: " + e.getMessage());
                        key.channel().close();
                        key.cancel();
                    }
                }
            }
        }
    }

    private static void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        // 새 연결을 수락한다.
        SocketChannel client = server.accept();

        if (client == null) {
            return ;
        }

        client.configureBlocking(false);

        // 각 연결은 독립적인 읽기 버퍼를 가진다.
        ConnectionState state = new ConnectionState();

        // 연결된 클라이언트의 읽기 이벤트를 감지한다.
        client.register(
                selector,
                SelectionKey.OP_READ,
                state
        );

        System.out.println("[ACCEPT] " + client.getRemoteAddress());
    }

    private static void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel)  key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        // 현재 읽을 수 있는 데이터만 읽는다.
        int bytesRead = client.read(state.readBuffer);

        if (bytesRead == -1) {
            client.close();
            key.cancel();
            return ;
        }

        if (bytesRead == 0) {
            return ;
        }

        System.out.println(
                "[READ] bytes=" + bytesRead +
                        ", thread=" + Thread.currentThread().getName()
        );

        // 기록된 데이터를 읽기 모드로 전환한다.
        state.readBuffer.flip();

        while(state.readBuffer.hasRemaining()) {
            byte value = state.readBuffer.get();

            if (value == '\n') {
                String message = state.message.toString().trim();
                System.out.println("[MESSAGE] " + message);

                // 응답은 쓰기 이벤트에서 전송한다.
                byte[] response = (
                        "ECHO: " + message + "\n"
                ).getBytes(StandardCharsets.UTF_8);

                state.writeBuffer = ByteBuffer.wrap(response);
                state.message.setLength(0);

                // 응답 전송이 끝날 때까지 읽기를 잠시 중단한다.
                key.interestOps(SelectionKey.OP_WRITE);

                break;
            }

            state.message.append((char) (value & 0xff));
        }

        // 아직 읽지 않은 바이트를 보존한다.
        state.readBuffer.compact();
    }

    private static void write(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();

        // Non-Blocking write 는 일부만 기록할 수 있다.
        client.write(state.writeBuffer);

        if (!state.writeBuffer.hasRemaining()) {
            // 전송이 끝났으면 다시 읽기 이벤트를 감시한다.
           state.writeBuffer = null;
           key.interestOps(SelectionKey.OP_READ);
        }


    }

    private static class ConnectionState {
        // 연결별 읽기 버퍼
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);

        // 줄바꿈까지 수신한 메시지를 누적한다.
        StringBuilder message = new StringBuilder();

        // 전송할 데이터가 있을때만 사용한다.
        ByteBuffer writeBuffer;
    }
}

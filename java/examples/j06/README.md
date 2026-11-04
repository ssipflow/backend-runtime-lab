# J-06 예제 실행·검증 — NIO TCP Echo

모든 명령은 **저장소 루트**에서 실행한다. JDK 17 이상과 nc가 필요하다. 공통 관찰 도구는 [Java 실행 안내](../../README.md)를 참고한다.

## 컴파일·실행: 터미널 A

```bash
mkdir -p build/examples/j06
javac --release 17 -d build/examples/j06 java/examples/j06/NioSelectorExample.java
java -cp build/examples/j06 j06.NioSelectorExample
```

8080 포트를 사용하므로 다른 실습 서버는 먼저 종료한다. 시작 로그에서 PID를 확인한다. 이 예제는 HTTP가 아닌 줄바꿈 기반 TCP 서버다.

## 메시지와 다수 연결: 터미널 B·C

각 터미널에서 연결하고 영문 메시지 한 줄을 입력한다.

```bash
nc localhost 8080
```

```text
hello
```

`ECHO: hello`가 반환되고 서버에 READ·MESSAGE 로그가 남는지 확인한다. 한 응답을 받은 뒤 다음 메시지를 보낸다. 여러 터미널의 연결을 유지한 채 각각 요청해도 요청 처리 Worker를 새로 만들지 않고 main이 처리하는지 확인한다.

## 부분 메시지

터미널 B에서 다음 명령을 실행한다.

```bash
python3 - <<'PYTHON'
import socket
import time

with socket.create_connection(("localhost", 8080), timeout=5) as client:
    client.sendall(b"hel")
    time.sleep(1)
    client.sendall(b"lo\n")
    response = bytearray()
    while not response.endswith(b"\n"):
        chunk = client.recv(1024)
        if not chunk:
            raise RuntimeError("응답 줄이 완성되기 전에 연결 종료")
        response.extend(chunk)
    print(response.decode(), end="")
PYTHON
```

줄바꿈까지 누적한 뒤 ECHO: hello가 반환되는지 확인한다. 데이터를 나누어 보내도 실제 read 횟수와 TCP 전달 경계는 고정되지 않는다.

## 관찰과 종료

`jps -l`에서 j06.NioSelectorExample의 PID를 찾고 아래 <PID>를 실제 숫자로 바꾼다.

```bash
top -pid <PID> -stats pid,command,threads,cpu,mem
jcmd <PID> Thread.print
```

유휴 시 main이 Selector.select의 OS 대기 경로에 있는지, 연결 수만큼 Worker가 생기지 않는지 확인한다. JVM 내부 스레드는 존재하며 RUNNABLE만으로 CPU 점유를 판단하지 않는다. nc와 서버는 각각 Ctrl+C로 종료하고 top은 q로 종료한다.

## 현재 구현의 범위

- ASCII 메시지 한 줄씩 확인한다. 바이트를 char로 바꾸는 코드는 완전한 UTF-8 디코더가 아니다.
- 한 번에 여러 줄을 보내면 compact로 보관한 나머지 메시지를 재처리하는 흐름이 부족해 응답이 지연될 수 있다.
- 부분 쓰기를 이어가는 코드는 있지만 작은 Echo 응답 성공만으로 부분 쓰기가 실제 발생했다고 단정하지 않는다.
- 메시지 길이·유휴 시간 제한이 없고, 현재 ServerSocketChannel은 try-with-resources에 포함되지 않는다. Selector 종료가 Channel 종료까지 대신하지 않는다.

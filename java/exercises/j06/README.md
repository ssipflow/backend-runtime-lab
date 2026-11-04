# J-06 연습문제 실행·검증 — NIO HTTP 서버

모든 명령은 **저장소 루트**에서 실행한다. JDK 17 이상, curl, Python 3가 필요하다. [Java 실행 안내](../../README.md)의 공통 도구와 포트·종료 안내를 함께 확인한다.

## 컴파일·실행: 터미널 A

```bash
mkdir -p build/exercises/j06
javac --release 17 -d build/exercises/j06 java/exercises/j06/NioSelectorExercise.java
java -cp build/exercises/j06 j06.NioSelectorExercise
```

8080을 쓰는 다른 서버는 종료한다. 시작 로그의 PID를 기록한다. 요청 처리용 Thread Pool 없이 main 이벤트 루프 하나가 ACCEPT·READ·WRITE를 처리한다.

## A. HTTP 응답: 터미널 B

```bash
curl -i --max-time 10 http://localhost:8080/fast
curl -i --max-time 10 http://localhost:8080/status
curl -i --max-time 10 http://localhost:8080/unknown
```

| 경로 | 확인할 결과 |
|---|---|
| /fast | 200, 본문 [FAST] OK와 줄바꿈 |
| /status | 200, activeConnections 값 |
| /unknown | 404, 본문 NOT FOUND와 줄바꿈 |

Content-Length가 UTF-8 본문 바이트 수와 맞고 응답 후 연결이 닫히는지 확인한다. 단독 status는 자신을 포함해 통상 1이다. 현재 status 응답은 Event Loop 이름을 포함하지 않는다.

## B. 요청 없는 연결 100개 유지

터미널 B에서 실행한다.

```bash
bash java/exercises/j06/concurrent_requests.sh
```

Python 3로 소켓 100개를 열어 요청을 보내지 않고 30초 유지한다. 연결 성공 메시지와 서버 ACCEPT 로그를 확인한다. 그동안 터미널 C에서 status를 요청하고 top·덤프로 비교한다.

```bash
curl -i --max-time 10 http://localhost:8080/status
jps -l
```

100개가 모두 수락되고 유지되는 동안 status가 처리되면 자신을 포함해 101이 예상된다. 수락·종료 시점에 따라 다를 수 있다. 스크립트 종료 후 별도 status는 통상 1로 돌아오는지 확인한다. 이것은 유휴 연결 실험이며 HTTP 요청 100개 처리량 실험은 아니다.

## C. 분할된 HTTP 요청

```bash
bash java/exercises/j06/partial_requests.sh
```

요청 줄, Host 헤더, 마지막 빈 줄을 1초 간격으로 보낸다. 헤더 종료 CRLF CRLF까지 누적한 뒤 200과 [FAST] OK를 반환하는지 확인한다. 스크립트는 recv를 한 번만 호출하므로 응답 전체가 항상 출력된다고 보장할 수 없다. 짧은 출력만으로 서버의 부분 읽기 실패를 단정하지 않는다. 아래 재확인 명령은 연결 종료까지 나누어 읽는다.

```bash
python3 - <<'PYTHON'
import socket
import time

with socket.create_connection(("localhost", 8080), timeout=5) as client:
    for part in (b"GET /fast HTTP/1.1\r\n", b"Host: localhost\r\n", b"\r\n"):
        client.sendall(part)
        time.sleep(1)
    chunks = []
    while True:
        chunk = client.recv(4096)
        if not chunk:
            break
        chunks.append(chunk)
    print(b"".join(chunks).decode(), end="")
PYTHON
```

송신 간격을 나눠도 실제 read 호출별 바이트 경계는 고정되지 않는다.

## D. Thread Dump와 CPU

jps에서 j06.NioSelectorExercise PID를 골라 <PID>를 실제 숫자로 바꾼다. 서버가 유휴일 때와 100개 연결 유지 중에 비교한다.

```bash
top -pid <PID> -stats pid,command,threads,cpu,mem
jcmd <PID> Thread.print
```

별도 요청 처리 Worker가 증가하지 않고, 유휴 main이 select에서 이벤트를 기다리는지 확인한다. macOS에서는 KQueue.poll 경로가 보일 수 있으나 JDK·OS에 따라 다르다. 전체 JVM 스레드 수가 1이어야 하는 것은 아니다.

덤프를 남기려면 저장소 루트에서 새 실행 디렉터리를 만든다.

```bash
mkdir -p build/runs/j06
run_dir=$(mktemp -d "$PWD/build/runs/j06/run.XXXXXX")
```

그다음 <PID>를 바꿔 실행한다.

```bash
jcmd <PID> Thread.print > "$run_dir/selector-thread-dump.txt"
```

산출물은 Git 제외 경로인 build/runs/j06에 남는다. top은 q, 서버는 실행 터미널의 Ctrl+C로 종료한다. 기존 연결 스크립트에는 연결 타임아웃이 없으므로 멈추면 해당 터미널에서 중단한다.

## 확인 범위와 현재 제약

- read의 0은 종료가 아니며 -1과 구분한다. 헤더는 여러 이벤트에 걸쳐 최대 8192바이트까지 누적하고 초과하면 연결을 닫는다.
- 응답 ByteBuffer에 남은 바이트가 있으면 OP_WRITE를 유지한다. 위 작은 응답 테스트만으로 실제 부분 쓰기를 강제 검증한 것은 아니다.
- Non-blocking I/O여도 이벤트 루프에서 긴 CPU 계산이나 Blocking 작업을 실행하면 다른 연결이 지연된다. 현재 서버에는 CPU 3초 경로가 없어 해당 지연을 실측했다고 표현하지 않는다.
- closeConnection의 무효 Key 조기 반환, 반복 toByteArray 복사, 유휴·요청 타임아웃 부재는 현재 구현의 제약이다. 모든 예외에서 자원 정리·루프 지속이 보장되는 것은 아니다.

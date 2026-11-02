# J-05 연습문제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

## 준비와 서버 실행

보조 TCP 서버의 Thread.ofPlatform 때문에 **JDK 21 이상**이 필요하다. HTTP 8080과 TCP 9090을 사용한다.

```bash
mkdir -p build/exercises/j05
javac --release 21 -d build/exercises/j05 \
  java/exercises/j05/CpuWaitExercise.java \
  java/exercises/j05subTCPServer/DelayTcpServer.java
```

터미널 A에서 [지연 TCP 서버](../j05subTCPServer/README.md)를 먼저 실행한다.

```bash
java -cp build/exercises/j05 j05subTCPServer.DelayTcpServer
```

터미널 B에서 HTTP 서버를 실행한다.

```bash
java -cp build/exercises/j05 j05.CpuWaitExercise
```

## 단일 요청 비교: 터미널 C

```bash
for endpoint in cpu wait io; do
  curl -s --max-time 15 -w "\n$endpoint: %{time_total}s\n" "http://localhost:8080/$endpoint"
done
curl -i --max-time 10 http://localhost:8080/status
```

세 작업 모두 약 3초지만 CPU는 cpu에서 높고 wait·io는 대기 위주인지 top으로 확인한다. io는 서버 로그에서 Received: 1도 확인한다. status는 pool·active·queue를 반환한다.

## 동시 요청과 큐

```bash
bash java/exercises/j05/concurrent_cpu_requests.sh
bash java/exercises/j05/concurrent_wait_requests.sh
bash java/exercises/j05/concurrent_io_requests.sh
bash java/exercises/j05/more_concurrent_wait_requests.sh
```

처음 세 명령은 각각 4개 요청이다. 마지막은 wait 8개로, Worker 4개가 첫 묶음을 처리한 뒤 다음 묶음을 처리하여 약 3초·6초 응답 그룹이 예상된다. core=max여도 Queue 20은 사용된다. less_concurrent_wait_requests.sh는 wait 3개를 보낸다.

## Thread Dump 수집

아래는 터미널 C를 저장소 루트에서 시작하는 서브셸 명령이다. 덤프를 build 아래에 생성하기 위해 작업 디렉터리를 바꾸며 부모 터미널의 위치는 유지한다. 같은 이름의 기존 덤프를 덮어쓰지 않도록 매번 새 디렉터리를 사용한다. HTTP 서버는 한 개만 실행되어 있어야 스크립트의 PID 자동 조회가 명확하다.

```bash
(
  repo_root="$PWD"
  mkdir -p build/runs/j05
  run_dir=$(mktemp -d "$repo_root/build/runs/j05/run.XXXXXX")
  cd "$run_dir" || exit
  bash "$repo_root/java/exercises/j05/dump_concurrent_cpu_requests.sh"
  bash "$repo_root/java/exercises/j05/dump_concurrent_wait_requests.sh"
  bash "$repo_root/java/exercises/j05/dump_concurrent_io_requests.sh"
  echo "덤프 위치: $run_dir"
)
```

각 스크립트는 요청 4개를 시작하고 1초 뒤 덤프를 저장한다. jcmd가 같은 JDK·사용자로 실행되는지 확인한다. 수집 시점에 작업이 끝났다면 다시 실행하여 Stack을 확인한다.

| 작업 | 확인할 상태 | Stack |
|---|---|---|
| cpu | RUNNABLE | cpuTask |
| wait | TIMED_WAITING | Thread.sleep |
| io | RUNNABLE 가능 | SocketDispatcher.read0 등 소켓 읽기 |

cpu·elapsed는 스레드 생존 기간의 누적값이다. 재사용 Worker의 값을 요청별 CPU 시간으로 읽지 않는다. CPU 실험은 경과 시간 기준 3초 부하이므로 고정 계산량 TPS 비교가 아니다.

## 종료와 현재 제약

HTTP와 TCP 서버를 각각 Ctrl+C로 종료한다. 요청 스크립트에는 타임아웃이 없으므로 응답이 멈추면 중단한다. 소켓 타임아웃도 없어서 보조 서버가 정상 동작하는지 먼저 확인한다. 현재 거절 응답의 숫자는 503이나 상태 문구는 Not Found로 남아 있다.

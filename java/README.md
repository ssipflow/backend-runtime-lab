# Java 실습 실행 안내

실행·재현 절차와 확인 기준을 코드와 함께 관리한다. 아래 예상 결과는 검증 기준이며 모든 환경에서 동일한 측정값을 보장하지 않는다.

## 공통 환경

- J-01~J-04, J-05 샘플, J-06~J-07: JDK 17 이상. 전체 실습을 같은 환경에서 실행하려면 JDK 21 이상을 사용한다.
- J-05 지연 TCP 서버: Thread.ofPlatform API를 사용하므로 JDK 21 이상.
- JDK의 java, javac, jps, jcmd와 curl을 PATH에서 사용할 수 있어야 한다.
- J-06은 Python 3(연결·분할 요청 스크립트)와 nc(TCP 예제)가 추가로 필요하다.
- 요청 스크립트는 Bash로 실행한다. top·ps 예시는 macOS 기준이다.
- 각 README의 명령은 별도 표시가 없으면 저장소 루트에서 실행한다. 컴파일은 -d로 패키지 디렉터리를 만들고 실행 시 패키지를 포함한 클래스명을 사용한다.

```bash
java -version
javac -version
```

## 챕터별 실행 문서

| 챕터 | 예제 | 연습문제 |
|---|---|---|
| J-01 Blocking I/O | [예제](examples/j01/README.md) | [순차 처리](exercises/j01/README.md) |
| J-02 Thread-per-Connection | [예제](examples/j02/README.md) | [활성 Worker](exercises/j02/README.md) |
| J-03 Thread Pool | [예제](examples/j03/README.md) | [큐·거절](exercises/j03/README.md) |
| J-04 Heap·GC | [예제](examples/j04/README.md) | [참조 유지·해제·OOM](exercises/j04/README.md) |
| J-05 CPU·대기 | [예제](examples/j05/README.md) | [CPU·타이머·I/O 비교](exercises/j05/README.md) |
| J-06 NIO Selector | [TCP Echo](examples/j06/README.md) | [NIO HTTP·분할 요청](exercises/j06/README.md) |
| J-07 JVM 진단 | [진단 샘플](examples/j07/README.md) | [Thread·Heap·JFR 분석](exercises/j07/README.md) |

J-05에는 별도 [지연 TCP 서버](exercises/j05subTCPServer/README.md)가 필요하다.

## 포트와 종료

J-01~J-03, J-05의 HTTP 서버와 J-06의 TCP 예제·HTTP 연습문제는 모두 8080을 사용한다. 한 번에 하나만 실행한다. J-05 보조 서버는 9090을 사용한다. 서버 실행 터미널은 열어 두고 요청·관찰은 다른 터미널에서 한다. 중단은 각 실행 터미널의 Ctrl+C이며, 정상 종료 절차를 검증하는 명령은 아니다. Address already in use가 나면 앞서 실행한 서버를 먼저 종료한다.

## JVM 관찰

```bash
jps -l
```

클래스명으로 대상 프로세스를 찾고 아래 <PID>를 실제 숫자로 바꾼다. HTTP와 TCP 프로세스를 혼동하지 않는다.

```bash
top -pid <PID> -stats pid,command,threads,cpu,mem
ps -M -p <PID>
jcmd <PID> Thread.print
```

top은 q로 종료한다. ps와 Thread Dump는 한 시점의 관찰이다. 여러 시점을 비교하고 Worker 이름·Stack·CPU 지표를 함께 읽는다. 전체 스레드에는 JVM 내부 스레드가 포함된다. RUNNABLE만으로 CPU 지속 점유를 판단하지 않는다.

## 실행 산출물

컴파일 클래스는 build/examples 또는 build/exercises, GC 로그·Heap Dump·Thread Dump는 build/runs 아래에 둔다. 이 경로는 Git 제외 대상이다. README와 실행 스크립트는 공유하되 측정 원본·덤프는 커밋하지 않는다. 재실행 때는 로그의 PID 또는 별도 실행 디렉터리로 기존 자료를 보존한다.

curl의 time_total은 클라이언트 전체 경과 시간이다. 서버의 처리 시작 로그는 큐 대기나 요청 읽기 전 시간을 포함하지 않을 수 있다. 스크립트의 curl 종료 코드만으로 HTTP 성공을 판단하지 말고 상태 코드와 본문·서버 로그를 함께 확인한다.

J-07 진단 스크립트는 build/runs 대신 java/exercises/j07/results/<유형>-<시각>에 결과를 저장한다. cpu·lock 스크립트는 macOS java_home의 JDK 17 도구를 선택한다. 자세한 환경과 수집 순서는 [J-07 진단 실습](exercises/j07/README.md)을 참고한다.

# J-02 연습문제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

```bash
mkdir -p build/exercises/j02
javac --release 17 -d build/exercises/j02 java/exercises/j02/ThreadPerConnectionServerExercise02.java
java -cp build/exercises/j02 j02.ThreadPerConnectionServerExercise02
```

## 동시 처리

```bash
curl -s --max-time 15 -w '\nslow: %{time_total}s\n' http://localhost:8080/slow &
sleep 0.3
curl -s --max-time 15 -w '\nfast: %{time_total}s\n' http://localhost:8080/fast
wait
```

slow 대기 중 다른 worker가 fast를 처리하는지 이름과 START/END 순서로 확인한다. fast가 slow 종료를 기다릴 필요가 없어야 한다.

## 활성 Worker와 OS 스레드

```bash
bash java/exercises/j02/concurrent_requests.sh
```

위 명령은 slow 10개를 요청한다. 실행 중 다른 터미널에서 호출한다.

```bash
curl -i --max-time 10 http://localhost:8080/status
```

10개 slow가 모두 활성이라면 status 자신을 포함해 11이 나올 수 있다. 요청 종료 후 단독 status는 통상 1이다. END 로그는 감소 전이므로 마지막 로그가 1이어도 누수로 단정하지 않는다.

```bash
bash java/exercises/j02/more_concurrent_requests.sh
```

100개 slow 실행 전·중·후 top의 threads와 cpu를 비교한다. 전체 JVM 스레드 수는 Worker 수와 다르며, sleep Worker 증가가 CPU 지속 점유를 뜻하지 않는다. 스크립트는 curl 타임아웃이 없으므로 응답이 없으면 중단한다.

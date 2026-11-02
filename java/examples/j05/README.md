# J-05 예제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

```bash
mkdir -p build/examples/j05
javac --release 17 -d build/examples/j05 java/examples/j05/CpuWaitExample.java
java -cp build/examples/j05 j05.CpuWaitExample
```

## 관찰

계산 Worker 2개와 sleep Worker 2개의 상태가 1초 간격으로 60회 출력된다. 계산은 주로 RUNNABLE, sleep은 주로 TIMED_WAITING인지 확인한다. PID는 시작 로그에 출력된다. 다른 터미널에서 top·jcmd로 CPU와 Stack을 함께 확인한다. 마지막에는 interrupt 후 join으로 종료한다. ThreadMXBean 객체만 만들고 CPU 시간을 조회하지는 않는다.

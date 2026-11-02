# J-02 예제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

```bash
mkdir -p build/examples/j02
javac --release 17 -d build/examples/j02 java/examples/j02/ThreadPerConnectionServer.java
java -cp build/examples/j02 j02.ThreadPerConnectionServer
```

## 동시 처리

```bash
curl -s --max-time 15 -w '\nslow: %{time_total}s\n' http://localhost:8080/slow &
sleep 0.3
curl -s --max-time 15 -w '\nfast: %{time_total}s\n' http://localhost:8080/fast
wait
```

slow 대기 중 다른 worker가 fast를 처리하는지 이름과 START/END 순서로 확인한다. fast가 slow 종료를 기다릴 필요가 없어야 한다.

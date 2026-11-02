# J-01 연습문제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

```bash
mkdir -p build/exercises/j01
javac --release 17 -d build/exercises/j01 java/exercises/j01/BlockingHttpServerExercise01.java
java -cp build/exercises/j01 j01.BlockingHttpServerExercise01
```

## 단일 요청

```bash
curl -i --max-time 10 http://localhost:8080/fast
curl -i --max-time 10 http://localhost:8080/slow
curl -i --max-time 10 http://localhost:8080/missing
```

각각 200/FAST, 약 3초 후 200/SLOW, 404를 확인한다.

## slow 처리 중 fast 요청

```bash
curl -s --max-time 15 -w '\nslow: %{time_total}s\n' http://localhost:8080/slow &
sleep 0.3
curl -s --max-time 15 -w '\nfast: %{time_total}s\n' http://localhost:8080/fast
wait
```

서버 로그에서 slow가 시작한 뒤 fast를 보냈는지 확인한다. 모두 main에서 처리하고 slow 종료 이후 fast가 시작해야 한다. fast의 전체 응답 시간에는 남은 slow 처리 대기가 포함된다. 도착 시점에 따라 달라지므로 항상 3초라고 기대하지 않는다. CPU 관찰은 공통 안내의 top을 사용한다.

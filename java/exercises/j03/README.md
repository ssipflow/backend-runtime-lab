# J-03 연습문제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

```bash
mkdir -p build/exercises/j03
javac --release 17 -d build/exercises/j03 java/exercises/j03/ThreadPoolHttpServerExercise03.java
java -cp build/exercises/j03 j03.ThreadPoolHttpServerExercise03
```

## 기본 응답과 상태

```bash
curl -i --max-time 10 http://localhost:8080/fast
curl -i --max-time 10 http://localhost:8080/slow
curl -i --max-time 10 http://localhost:8080/status
```

fast는 FAST, slow는 약 3초 후 SLOW를 반환한다. 설정은 core=2, max=4, queue=3이다. status도 같은 Pool에서 처리되어 대기하거나 거절될 수 있다. active는 CPU 실행 중인 스레드 수가 아니다.

## Worker 재사용

```bash
for round in 1 2; do
  for i in 1 2; do curl -s --max-time 15 http://localhost:8080/slow > /dev/null & done
  wait
done
```

두 묶음의 로그에서 worker 이름이 재사용되는지 확인한다. 매 요청마다 새로운 이름이 만들어지는 구조와 비교한다.

## 큐·거절과 응답 시간

```bash
bash java/exercises/j03/concurrent_requests.sh
```

8개 slow 요청의 HTTP 코드와 curl 전체 시간을 확인한다. 충분히 겹쳐 수락되면 200 네 개는 약 3초, 200 세 개는 약 6초, 하나는 503이 예상된다. 요청 번호는 수락 순서가 아니므로 항상 8번이 거절되지는 않는다.

```bash
bash java/exercises/j03/more_concurrent_requests.sh
```

이 스크립트는 **status 100개**를 요청한다. slow 100개 실험이 아니다. 순간 부하와 스케줄링에 따라 거절 개수는 달라진다. status 본문의 pool·active·queue·completed를 확인한다. completed는 서버 시작 이후 누적값이며 status 자신은 아직 완료 전이다.

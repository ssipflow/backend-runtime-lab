# J-03 예제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

```bash
mkdir -p build/examples/j03
javac --release 17 -d build/examples/j03 java/examples/j03/ThreadPoolHttpServer.java
java -cp build/examples/j03 j03.ThreadPoolHttpServer
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

## 포화 동작

```bash
bash java/exercises/j03/concurrent_requests.sh
```

8개 slow에 대해 4개 실행·3개 큐·1개 거절을 관찰한다(앞선 작업이 끝나기 전 수락된 조건). 샘플은 거절된 연결을 응답 없이 닫으므로 curl HTTP 000이나 전송 오류가 나올 수 있다. 연습문제의 503 동작과 구분한다.

## 현재 코드의 제약

기본 경로는 `send(client, "NOT FOUND" + 404)`여서 404 대신 200과 NOT FOUND404 본문을 보낸다. 이 README는 현재 동작을 기술하며 코드를 수정하지 않는다.

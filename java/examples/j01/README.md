# J-01 예제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

```bash
mkdir -p build/examples/j01
javac --release 17 -d build/examples/j01 java/examples/j01/BlockingHttpServer.java
java -cp build/examples/j01 j01.BlockingHttpServer
```

## 요청과 검증

```bash
curl -i --max-time 10 http://localhost:8080/
```

200 응답, 본문 `Hello Java Server`, 본문의 바이트 수에 맞는 Content-Length를 확인한다. 서버 로그에서 연결·요청·종료 순서를 확인한다. 요청을 보내지 않으면 accept 또는 읽기에서 기다리는 것은 정상이다.

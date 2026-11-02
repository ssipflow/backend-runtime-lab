# J-05 지연 TCP 서버

JDK 21 이상, TCP 9090을 사용한다. 모든 명령은 저장소 루트 기준이다.

```bash
mkdir -p build/exercises/j05
javac --release 21 -d build/exercises/j05 java/exercises/j05subTCPServer/DelayTcpServer.java
java -cp build/exercises/j05 j05subTCPServer.DelayTcpServer
```

연결마다 별도의 플랫폼 스레드를 만들어 약 3초 기다린 뒤 값 1인 1바이트를 보내고 닫는다. HTTP Worker Pool과 별도 자원으로 진행된다. [HTTP 서버와 함께 실행·검증](../j05/README.md)에서 실제 read 대기를 관찰한다. 종료는 실행 터미널의 Ctrl+C다.

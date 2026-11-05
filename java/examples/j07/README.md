# J-07 예제 실행 — JVM 진단 대상

모든 명령은 저장소 루트 기준이다. JDK 17 이상이 필요하며 포트는 사용하지 않는다. [Java 공통 안내](../../README.md)와 [진단 연습문제](../../exercises/j07/README.md)를 함께 참고한다.

## 컴파일·실행: 터미널 A

```bash
mkdir -p build/examples/j07
javac --release 17 -d build/examples/j07 java/examples/j07/DiagnosticExample.java
java -Xms64m -Xmx256m -cp build/examples/j07 j07.DiagnosticExample
```

시작 로그의 PID를 기록하고 콘솔을 유지한다. 진단 스크립트는 별도 터미널에서 실행한다.

| 콘솔 명령 | 실제 동작과 확인 기준 |
|---|---|
| cpu | Worker 4개가 약 4초 동안 반복 계산. CPU 부하와 RUNNABLE 계산 Stack 관찰 |
| lock | Worker 4개가 같은 Monitor 안에서 각각 5초 sleep. 소유자 1개와 BLOCKED 대기 관찰 |
| heap | 정적 List에 1MiB 배열 10개 추가 |
| clear | List의 배열 참조 제거 |
| status | 보관 배열 수, Used·Committed Heap 출력 |
| exit | main 입력 루프 종료 |

CPU 작업은 현재 코드 기준 4초다. 진단 수집은 작업 시작 직후 진행한다. 고정 계산량이 아니라 경과 시간 기준 부하이므로 정확한 CPU 실행 시간과 구분한다.

## 기본 콘솔 동작 확인

한 줄씩 입력한다.

```text
status
heap
status
clear
status
```

retained가 0 → 10 → 0으로 바뀌는지 확인한다. clear 직후 Used가 줄지 않아도 참조 해제 실패라고 단정하지 않는다. 실제 메모리 회수는 GC 시점과 구분한다.

CPU·락 실험을 겹쳐 실행하면 원인 해석이 어려워지므로 한 실험이 끝난 뒤 다음을 시작한다. 같은 명령을 반복하면 새 Worker가 생성되고 이름이 중복될 수 있다. CPU Worker는 약 4초, 락 Worker 전체는 경합으로 약 20초가 필요할 수 있다.

## 종료

콘솔에서 exit를 입력하거나 Ctrl+C로 중단한다. exit는 이미 시작한 Worker를 중단시키지 않으므로 비데몬 Worker가 끝날 때까지 JVM이 남을 수 있다. 별도 종료 신호·정상 종료 동작을 검증한 구현은 아니다.

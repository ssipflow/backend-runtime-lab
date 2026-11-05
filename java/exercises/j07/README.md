# J-07 연습문제 — JVM 진단 도구 사용과 분석

이번 연습문제는 [DiagnosticExample](../../examples/j07/README.md)을 실행하고 Thread Dump·JFR·Heap 자료로 원인을 설명하는 실습이다. 별도의 연습문제 Java 서버는 없다. 모든 명령은 저장소 루트 기준이다.

## 준비

터미널 A에서 예제 README의 컴파일·실행 명령으로 JVM을 시작한다. 터미널 B에서 아래 명령으로 대상 PID를 확인한다.

```bash
jps -l
```

클래스명이 j07.DiagnosticExample인지 확인한다. 스크립트는 PID를 인자로 받으며 생략하면 첫 일치 JVM을 자동 선택한다. 아래 <PID>는 대상 숫자로 바꾼다. 동일 클래스를 여러 JVM에서 실행 중이면 명시적으로 전달한다.

cpu-test.sh·lock-test.sh는 macOS의 java_home으로 JDK 17 도구를 선택하므로 해당 JDK가 설치되어 있어야 한다. heap-test.sh는 현재 PATH의 도구를 사용한다. 대상 JVM과 도구 버전을 확인하고, attach 실패 시 도구 경로·사용자를 점검한다.

## A. CPU 진단

1. 터미널 B에서 스크립트를 실행해 입력 대기 상태로 둔다.

```bash
bash java/exercises/j07/cpu-test.sh <PID>
```

2. 터미널 A 콘솔에 cpu를 입력한다.
3. 즉시 터미널 B에서 Enter를 누른다. 현재 CPU 작업은 4초이므로 지체하면 수집 구간을 놓칠 수 있다.
4. 스크립트가 Thread Dump, 10초 JFR을 수집하고 12초 뒤 요약·실행 샘플을 추출한다.

| 결과 파일 | 확인할 내용 |
|---|---|
| cpu-thread-dump.txt | cpu-worker-* 상태, 계산 람다 Stack, cpu·elapsed |
| cpu-test.jfr | 일정 기간의 이벤트 원본 |
| cpu-jfr-summary.txt | 기록 시간·이벤트 유형별 건수 |
| cpu-execution-samples.txt | jdk.ExecutionSample의 sampledThread·stackTrace |

RUNNABLE만 보지 말고 계산 Stack과 CPU 시간, 실행 샘플이 같은 원인을 가리키는지 확인한다. lambda$startCpuWorkers가 반복되는지 조사한다. summary의 NativeLibrary 건수는 CPU 사용 메서드 순위가 아니다. 샘플의 최초·마지막 startTime 차이도 CPU Time이 아니다. cpu는 스레드 생성 후 누적값이다.

실행 샘플이 없다면 기록 시점이 부하와 겹쳤는지, 이벤트 설정과 기록 완료 여부를 확인한다. 작은 샘플 수만으로 정밀한 CPU 비중을 산출하지 않는다.

## B. Monitor 경합

1. 터미널 B에서 실행한다. 이 스크립트는 먼저 JFR 25초 기록을 시작한다.

```bash
bash java/exercises/j07/lock-test.sh <PID>
```

2. JFR 시작 안내를 본 뒤 터미널 A에 lock을 입력한다.
3. 즉시 터미널 B에서 Enter를 눌러 Thread Dump를 수집한다.
4. 27초 대기 후 summary와 Monitor 이벤트 추출 완료를 확인한다. 시작 후 lock 입력까지 오래 기다리면 기록이 부하보다 먼저 끝날 수 있다.

| 결과 파일 | 확인할 내용 |
|---|---|
| lock-thread-dump.txt | 소유자의 locked와 대기자의 waiting to lock이 같은 Monitor인지 |
| lock-test.jfr | 경합 기록 원본 |
| lock-jfr-summary.txt | 기록·이벤트 요약 |
| lock-monitor-events.txt | JavaMonitorEnter의 eventThread·previousOwner·duration·Stack |

소유자는 Monitor를 보유한 채 sleep해 TIMED_WAITING, 다른 Worker는 BLOCKED일 수 있다. sleep은 synchronized Monitor를 해제하지 않는다. 소유자가 반드시 worker-1인 것은 아니다. JFR 출력 순서를 모든 Lock 획득의 완전한 순서로 단정하지 않는다.

## C. Heap 점유와 참조 유지

터미널 B에서 실행한다.

```bash
bash java/exercises/j07/heap-test.sh <PID>
```

터미널 A 콘솔에 아래를 입력한다.

```text
heap
heap
heap
heap
heap
status
```

50개 배열 보관을 확인한 뒤 터미널 B에서 Enter를 누른다. Heap Histogram과 Heap Dump를 수집한다. 기존 보관 데이터가 있는 JVM이라면 새 JVM으로 시작하거나 clear로 상태를 맞춘다.

| 결과 파일 | 확인할 내용 |
|---|---|
| heap-histogram.txt | [B(byte 배열)의 개수·바이트 점유 |
| heap-test.hprof | 객체 그래프·GC Root 참조 경로 분석용 자료 |

그다음 터미널 A에서 clear와 status를 입력하고 보관 개수가 0인지 확인한다. 스크립트는 clear 후 결과를 자동 수집하지 않는다. GC 전후 회수량을 비교하려면 별도 시점의 관찰이 필요하다. Histogram·Heap Dump 수집 자체가 GC·정지에 영향을 줄 수 있으므로 자연스러운 회수 실험과 구분한다.

Histogram은 클래스별 합계이며 모든 byte 배열이 실습의 1MiB 배열은 아니다. 큰 점유만으로 누수라고 판정하지 말고 정적 RETAINED → List → 배열 경로와 의도된 보관 여부를 확인한다. 덤프 생성과 덤프 내부 분석 완료도 별개다.

## 산출물과 재실행

스크립트는 자신의 디렉터리로 이동하므로 실행 위치와 관계없이 아래에 저장한다.

```text
java/exercises/j07/results/cpu-<YYYYmmdd-HHMMSS>/
java/exercises/j07/results/lock-<YYYYmmdd-HHMMSS>/
java/exercises/j07/results/heap-<YYYYmmdd-HHMMSS>/
```

콘솔에 출력된 디렉터리에서 결과를 확인한다. 같은 유형은 한 번에 하나씩 실행하고 이전 기록이 끝난 뒤 재실행한다. 디렉터리 이름은 초 단위이므로 같은 초에 재실행해 기존 결과를 덮어쓰지 않도록 한다. JFR 기록명도 같은 JVM에서 중복될 수 있다.

셸 스크립트와 README는 공유하되 .txt·.jfr·.hprof 결과는 Git 제외 대상으로 유지한다. Heap Dump는 로컬에서 분석한다. 공통 안내의 build/runs와 달리 현재 J-07 스크립트는 위 results 경로를 사용한다.

## 분석 기록의 기준

각 실험에서 증상 → 수집한 증거 → 원인 가설 → 추가 확인 사항을 적는다. 낮은 CPU와 높은 Active Count라면 Pool 지표 → Worker의 대기 Stack → I/O·락 가설 → 관련 JFR 이벤트 순서로 확인한다. 여기의 콘솔 샘플 자체는 Thread Pool 서버가 아니므로 해당 상황을 직접 재현하는 것은 아니다.

종료는 예제 콘솔의 exit 또는 Ctrl+C를 사용한다. exit 후 Worker 종료까지 JVM이 유지될 수 있다. 이 문서의 검증 기준은 예상 동작이며 실행 환경에서 새로 측정한 값으로 대체해 해석한다.

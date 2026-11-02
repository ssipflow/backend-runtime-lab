# J-04 예제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

## 컴파일

```bash
mkdir -p build/examples/j04 build/runs/j04
javac --release 17 -d build/examples/j04 java/examples/j04/HeapGcExample.java
```

## G1·Heap 제한으로 실행

```bash
java -Xms64m -Xmx128m -XX:+UseG1GC \
  "-Xlog:gc*:file=build/runs/j04/examples-%p.log:time,uptime,level,tags" \
  -cp build/examples/j04 j04.HeapGcExample
```

로그 파일의 %p는 JVM PID로 치환된다. 생성된 파일 목록은 `ls build/runs/j04`로 확인한다. 다른 터미널에서 `jps -l`로 PID를 확인하고 아래 자리표시자를 실제 숫자로 바꾼다.

```bash
jcmd <PID> GC.heap_info
```

GC 로그의 전후 Used와 괄호 안 Heap 용량, Region 변화를 확인한다. Used 감소가 Committed나 RSS 감소를 보장하지 않는다. 히스토그램은 `jcmd <PID> GC.class_histogram`으로 볼 수 있지만 GC·정지를 유발해 자연스러운 회수 시점 관찰에 영향을 줄 수 있으므로 비교 실험과 구분한다.

## 관찰 기준

임시 배열 생성 후 정적 List에 배열 40개를 보관한다. RETAINED 로그의 증가와 GC 후에도 보관 객체가 남는지 확인한다. 약 8초의 보관 할당 이후 60초 대기하고 종료한다. 현재 코드는 AFTER TEMPORARY를 따로 출력하지 않는다. 1MiB 배열의 G1 점유는 Region 크기와 객체 헤더에 따라 데이터 크기보다 커질 수 있다.

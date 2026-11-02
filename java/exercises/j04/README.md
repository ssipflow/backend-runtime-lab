# J-04 연습문제 실행·검증

모든 명령은 **저장소 루트**에서 실행한다. 서버 실행 터미널은 유지하고 요청은 별도 터미널에서 보낸다. 공통 도구·종료 방법은 [Java 실행 안내](../../README.md)를 참고한다.

## 컴파일

```bash
mkdir -p build/exercises/j04 build/runs/j04
javac --release 17 -d build/exercises/j04 java/exercises/j04/HeapGcExercise.java
```

## G1·Heap 제한으로 실행

```bash
java -Xms64m -Xmx128m -XX:+UseG1GC \
  "-Xlog:gc*:file=build/runs/j04/exercises-%p.log:time,uptime,level,tags" \
  -cp build/exercises/j04 j04.HeapGcExercise
```

로그 파일의 %p는 JVM PID로 치환된다. 생성된 파일 목록은 `ls build/runs/j04`로 확인한다. 다른 터미널에서 `jps -l`로 PID를 확인하고 아래 자리표시자를 실제 숫자로 바꾼다.

```bash
jcmd <PID> GC.heap_info
```

GC 로그의 전후 Used와 괄호 안 Heap 용량, Region 변화를 확인한다. Used 감소가 Committed나 RSS 감소를 보장하지 않는다. 히스토그램은 `jcmd <PID> GC.class_histogram`으로 볼 수 있지만 GC·정지를 유발해 자연스러운 회수 시점 관찰에 영향을 줄 수 있으므로 비교 실험과 구분한다.

## 참조 유지·제거·회수

실행 터미널에 아래 명령을 한 줄씩 입력한다. allocate 한 번에 배열 10개를 보관한다.

```text
allocate
allocate
allocate
allocate
allocate
status
release
status
allocate
status
allocate
status
exit
```

처음 status에서 50개, release 뒤 0개, 이후 10개·20개인지 확인한다. clear 직후 Used가 그대로여도 정상이다. 이후 GC가 실제 발생한 구간에서 로그의 Used·Humongous Region 감소를 확인한다. 특정 명령에서 반드시 GC가 발생하거나 특정 수치가 나오는 것은 보장하지 않는다. System.gc로 강제하지 않는다.

## 별도 JVM에서 OOM 재현

```bash
java -Xms64m -Xmx128m -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=build/runs/j04 \
  "-Xlog:gc*:file=build/runs/j04/oom-%p.log:time,uptime,level,tags" \
  -cp build/exercises/j04 j04.HeapGcExercise
```

새 프로세스에서 release 없이 allocate를 반복한다. Java heap space OOM과 덤프 생성 메시지를 확인한다. 발생 횟수는 고정하지 않는다. 덤프는 build/runs/j04의 java_pid<PID>.hprof에 생성된다. 같은 이름이 이미 있으면 JVM이 덤프를 쓰지 못할 수 있다. 덤프는 로컬에서만 분석하고 Git에 추가하지 않는다.

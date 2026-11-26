량# HEAP Profile 분석 실험 방법

1. 초기 메모리 상태 확인
 ```bash
curl http://localhost:8080/stats
 ```

2. Heap Profile을 파일로 저장
```bash
curl -s \
   http://localhost:6060/debug/pprof/heap?gc=1 \
   -o heap-before.pb.gz
```
   
3. `/heap` 요청 100회 실행
```bash
for i in {1..100}; do
   curl -s http://localhost:8080/heap > /dev/null
done
```

4. 이후 메모리 상태와 프로파일 수집
```bash
curl http://localhost:8080/stats

curl -s \
  http://localhost:6060/debug/pprof/heap?gc=1 \
  -o heap-after.pb.gz
```

5. 분석 명령어
```bash
go tool pprof -sample_index=inuse_space heap-after.pb.gz
```

6. pprof 내부에서 다음 명령어 실행
```bash
(pprof) top
(pprof) list heapHandler
```
이번에는 전역 Slice 가 약 100MiB 의 버퍼를 유지하므로 `inuse_space`에서 관련 할당을 관찰할 수 있어야 한다.

7. 이어서 누적 할당량을 분석한다.
```bash
go tool pprof -sample_index=alloc_space heap-after.pb.gz

```
두 결과를 비교하면서 누적 할당량과 현재 살아있는 객체의 차이를 확인한다.
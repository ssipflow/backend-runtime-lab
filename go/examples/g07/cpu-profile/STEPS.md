# CPU Profile 분석 실험 방법

1. 서버 실행
```bash
go run ./go/examples/g07/main.go
```

2. 터미널 B에서 CPU 요청 반복
```bash
for i in {1..100}; do 
  curl -s http://localhost:8080/cpu > /dev/null
done
```

3. 요청이 실행되는 동안 터미널 C 에서 CPU Profile을 수집한다.
```bash
go tool pprof \
  -seconds 10 \
  http://127.0.0.1:6060/debug/pprof/profile
```

4. pprof 대화형 화면에서 다음 명령어를 실행한다.
```bash
(pprof) top
(pprof) list calculate
(pprof) peek calculate
```

`top`은 CPU 샘플이 많이 수집된 함수를 보여주고, `list calculate` 는 소스코드의 어느 줄에서 CPU 시간이 소비되었는지 보여준다.

예상 관찰결과는 `calculate()`의 반복 연산이 CPU Profile에서 큰 비중을 차지하는 것이다.

실제 비중은 요청 수, 실행환경, 샘플 수에 따라 달라진다.

# Goroutine Profile 분석 실험 방법

1. 초기 Goroutine 수를 확인한다.
```bash
curl http://localhost:8080/stats
```

2. 이후 종료되지 않는 Goroutine을 100개 생성한다.
```bash
for i in {1..100}; do
  curl http://localhost:8080/goroutine > /dev/null
done
```

3. Goroutine 수를 다시 확인한다.
```bash
curl http://localhost:8080/stats
```

4. Goroutine Profile을 수집한다.
```bash
curl -s \
  http://localhost:6060/debug/pprof/goroutine?debug=2 \
  -o goroutines.txt
```

`goroutines.txt`에서 `goroutineLeak.func1`을 겸색하면 Channel 수신을 기다리는 Goroutine의 Stack Trace를 확인할 수 있다.

여기서 두가지를 구분해야 한다.

- `runtime.NumGorouine()`은 현재 Goroutine의 전체 개수를 보여준다
- Goroutine Profile 은 해당 Goroutine이 어떤 호출 스택에서 실행대기 또는 대기중인지 보여준다.

따라서 개수 증가만 확인하는 것보다 Stack Trace 까지 확인하는 것이 원인분석에 유용하다.
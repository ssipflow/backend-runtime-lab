package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	_ "net/http/pprof"
	"runtime"
	"time"
)

/**
G-07 문제: 성능 저하와 Goroutine Leak 진단
다음 두 가지 문제가 있는 서버를 직접 구현하고 pprof로 분석합니다.

요구사항
1. GET /process는 CPU 연산을 수행합니다.
2. GET /leak는 종료되지 않는 Goroutine을 하나 생성합니다.
3. GET /stats는 Goroutine 수와 Heap 사용량을 출력합니다.
4. CPU Profile에서 연산 병목 함수를 식별합니다.
5. Goroutine Profile에서 Leak 발생 함수를 식별합니다.
6. Leak을 수정한 뒤 같은 요청을 반복해 Goroutine 수가 계속 증가하지 않는지 확인합니다.

이번 문제에서는 샘플의 전역 Heap 유지 코드를 그대로 사용하지 않습니다. Heap Profile 분석은 앞선 Example에서 수행합니다.
*/

// CPU 연산 함수
func processData() uint64 {
	var result uint64 = 1

	// TODO:
	// 충분한 CPU 사용량을 발생시키는 반복 연산을 작성한다
	// 결과가 호출자에게 반환되도록 구현한다.
	for i := range 50_000_000 {
		result = result*1664525 + uint64(i) + 1013904223
	}

	return result
}

func processHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: processData()를 호출하고 결과를 응답한다.
	result := processData()
	fmt.Fprintln(w, result)
}

func leakHandler(w http.ResponseWriter, r *http.Request) {
	// TODO:
	// Channel 수신을 기다리는 Goroutine을 생성한다.
	// 최초 구현에서는 의도적으로 종료 경로를 만들지 않는다.
	ch := make(chan struct{})
	ctx, cancel := context.WithTimeout(
		context.Background(),
		3*time.Second,
	)

	go func() {
		defer cancel()

		select {
		case <-ch:
			log.Println("Channel Received")

		case <-ctx.Done():
			log.Println("Goroutine terminated:", ctx.Err())
		}
	}()

	fmt.Fprintln(w, "Goroutine created")
}

func statsHandler(w http.ResponseWriter, r *http.Request) {
	var stats runtime.MemStats
	runtime.ReadMemStats(&stats)

	// TODO:
	// runtime.NumGoroutine()을 출력한다.
	// stats.HeapAlloc과 stats.NumGC를 출력한다.
	fmt.Fprintf(w, "NumGoroutine: %d\n", runtime.NumGoroutine())
	fmt.Fprintf(w, "HeapAlloc: %d\n", stats.HeapAlloc)
	fmt.Fprintf(w, "NumGC: %d\n", stats.NumGC)
}

func main() {
	// pprof 서버
	go func() {
		log.Println("pprof listening on :6060")

		server := &http.Server{
			Addr:    "127.0.0.1:6060",
			Handler: http.DefaultServeMux,
		}

		log.Println(server.ListenAndServe())
	}()

	mux := http.NewServeMux()
	mux.HandleFunc("/process", processHandler)
	mux.HandleFunc("/leak", leakHandler)
	mux.HandleFunc("/stats", statsHandler)

	log.Println("Application listening on :8080")
	server := &http.Server{
		Addr:    "127.0.0.1:8080",
		Handler: mux,
	}

	// TODO:
	// HTTP 서버를 실행한다.
	// 서버 실행 오류를 기록한다.
	log.Fatal(server.ListenAndServe())
}

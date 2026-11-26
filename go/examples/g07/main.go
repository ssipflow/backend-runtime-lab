package main

import (
	"fmt"
	"log"
	"net/http"
	_ "net/http/pprof"
	"runtime"
	"sync"
	"time"
)

/**
# Example - CPU, Heap, Goroutine을 한 서버에서 관찰
- `/cpu`: CPU 연산을 반복한다.
- `/heap`: 메모리를 할당하고 전역변수에 유지한다.
- `/goroutine`: 종료되지 않는 Goroutine 을 생성한다.
프로파일링용 HTTP 서버는 별도 포트 6060에서 실행한다.
*/

// 전역 참조를 유지하여 GC가 객체를 회수하지 못하게 한다.
var (
	retainedMu sync.Mutex
	retained   [][]byte
)

// CPU 사용량을 발생시키는 함수
// 연산 결과를 HTTP 응답에 사용하여 계산이 관찰 가능하도록 한다.
func calculate(iterations int) uint64 {
	var result uint64 = 1

	for i := range iterations {
		result = result*1664525 + uint64(i) + 1013904223
	}

	return result
}

func cpuHandler(w http.ResponseWriter, r *http.Request) {
	result := calculate(50_000_000)
	fmt.Fprintln(w, result)
}

// 요청마다 1MiB를 할당하고 전역 Slice 에 보관한다.
func heapHandler(w http.ResponseWriter, r *http.Request) {
	data := make([]byte, 1024*1024)

	// 실제 메모리 페이지를 사용하도록 데이터 기록
	for i := range data {
		data[i] = byte(i)
	}

	retainedMu.Lock()
	retained = append(retained, data)
	count := len(retained)
	retainedMu.Unlock()

	fmt.Fprintf(w, "Retained buffers: %d\n", count)
}

// 수신자가 영원히 대기하는 Goroutine 을 만든다.
func goroutineLeak(w http.ResponseWriter, r *http.Request) {
	ch := make(chan struct{})

	go func() {
		// 송신자도 없고 Channel 을 닫는 코드도 없다
		// 따라서 이 Goroutine은 종료되지 않는다.
		<-ch
	}()

	fmt.Fprintln(w, "Goroutine created")
}

func statsHandler(w http.ResponseWriter, r *http.Request) {
	var status runtime.MemStats
	runtime.ReadMemStats(&status)

	fmt.Fprintf(w, "Goroutines: %d\n", runtime.NumGoroutine())
	fmt.Fprintf(w, "HeapAlloc: %d bytes\n", status.HeapAlloc)
	fmt.Fprintf(w, "HeapObjects: %d\n", status.HeapObjects)
	fmt.Fprintf(w, "TotalAlloc: %d bytes\n", status.TotalAlloc)
	fmt.Fprintf(w, "NumGC: %d\n", status.NumGC)
}

func main() {
	// pprof 디버그 엔드포인트를 별도 서버에서 제공한다.
	// net/http/pprof는 DefaultServerMux에 Handler를 등록한다.
	go func() {
		log.Println("pprof listening on :6060")

		server := &http.Server{
			Addr:              "127.0.0.1:6060",
			Handler:           http.DefaultServeMux,
			ReadHeaderTimeout: 5 * time.Second,
		}

		if err := server.ListenAndServe(); err != nil {
			log.Println("pprof server stopped:", err)
		}
	}()

	mux := http.NewServeMux()
	mux.HandleFunc("/cpu", cpuHandler)
	mux.HandleFunc("/heap", heapHandler)
	mux.HandleFunc("/goroutine", goroutineLeak)
	mux.HandleFunc("/stats", statsHandler)

	log.Println("Application listening on :8080")
	server := &http.Server{
		Addr:              ":8080",
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
	}

	log.Fatal(server.ListenAndServe())
}

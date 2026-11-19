package main

import (
	"encoding/json"
	"log"
	"net/http"
	"runtime"
	"time"
)

type Result struct {
	Total uint64 `json:"total"`
}

func handlerErr(err error, w http.ResponseWriter) {
	log.Printf("Handler error: %v", err)
	http.Error(w, "Internal Server Error", http.StatusInternalServerError)
}

func cpuHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: 고정된 횟수의 CPU 연산 수행
	result := Result{
		Total: 0,
	}
	for range 2_000_000 {
		result.Total++
	}

	bytes, err := json.Marshal(result)
	if err != nil {
		handlerErr(err, w)
		return
	}

	// TODO: 계산 결과를 HTTP 응답으로 반환
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)

	if _, err := w.Write(bytes); err != nil {
		log.Printf("Response write failed %v", err)
	}
}

func ioHandler(w http.ResponseWriter, r *http.Request) {
	// TODO: 200ms 대기
	time.Sleep(200 * time.Millisecond)

	// TODO: "ok"를 HTTP 응답으로 반환.
	// TODO: 계산 결과를 HTTP 응답으로 반환
	message := "ok"

	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.WriteHeader(http.StatusOK)

	if _, err := w.Write([]byte(message)); err != nil {
		log.Printf("Response write failed %v", err)
	}
}

func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)
	log.Println("GOMAXPROCS:", runtime.GOMAXPROCS(0))

	http.HandleFunc("/cpu", cpuHandler)
	http.HandleFunc("/io", ioHandler)

	log.Println("Listen on :8080")

	if err := http.ListenAndServe(":8080", nil); err != nil {
		panic(err)
	}
}

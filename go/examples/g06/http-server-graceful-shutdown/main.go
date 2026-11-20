package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

// 처리 시간이 3초인 HTTP 요청
func shutdownHandler(w http.ResponseWriter, r *http.Request) {
	log.Println("Request Started")

	// 실제 외부 API 호출이나 DB 작업을 가정한다.
	select {
	case <-time.After(3 * time.Second):
		_, _ = fmt.Fprintln(w, "completed")
		log.Println("Request completed")

	case <-r.Context().Done():
		// 클라이언트가 요청을 취소한 경우
		log.Println("Request canceled:", r.Context().Err())
	}
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/slow", shutdownHandler)

	server := &http.Server{
		Addr:    ":8080",
		Handler: mux,
	}

	// SIGINT 또는 SIGTERM을 Context 취소로 변환한다.
	signalCtx, stop := signal.NotifyContext(
		context.Background(),
		os.Interrupt,
		syscall.SIGTERM,
	)
	defer stop()

	// HTTP 서버는 별도 Goroutine 에서 실행한다.
	serverDone := make(chan error, 1)
	go func() {
		err := server.ListenAndServe()
		serverDone <- err
	}()

	log.Println("Server started on :8080")

	// 종료 신호 또는 서버 자체 오류를 기다린다.
	select {
	case <-signalCtx.Done():
		log.Println("Shutdown signal received")

	case err := <-serverDone:
		if !errors.Is(err, http.ErrServerClosed) {
			log.Println("Server failed:", err)
		}
		return
	}

	// 종료 신호 감지를 중단한다.
	stop()

	// 기존 요청을 최대 5초동안 기다린다.
	shutdownCtx, cancel := context.WithTimeout(
		context.Background(),
		5*time.Second,
	)
	defer cancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Println("Graceful shutdown failed:", err)

		// 정상 종료에 실패한 경우 남은 연결을 닫는다.
		if closeErr := server.Close(); closeErr != nil {
			log.Println("server close failed:", closeErr)
		}
	}

	log.Println("Server stopped")
}

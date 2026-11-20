package main

import (
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"sync"
	"sync/atomic"
	"syscall"
	"time"
)

/**
G-06 문제: 작업 유실을 방지하는 Graceful Shutdown
HTTP Server · Background Worker · WaitGroup · Shutdown Timeout

HTTP 요청으로 작업을 등록하고, 백그라운드 Worker가 이를 처리하는 서버를 구현합니다.

요구사항
1. POST /jobs로 작업을 등록합니다.
2. 작업은 Buffered Channel에 저장합니다.
3. Worker 2개가 작업을 처리합니다.
4. 작업 하나의 처리 시간은 500ms입니다.
5. SIGINT 또는 SIGTERM을 받으면 종료 절차를 시작합니다.
6. 종료가 시작되면 신규 HTTP 요청 수락을 중단합니다.
7. 이미 등록된 작업은 모두 처리한 후 Worker를 종료합니다.
8. 전체 종료 제한 시간은 5초입니다.
9. 정상 종료 시 등록된 작업 수와 완료된 작업 수가 같아야 합니다.

제약조건
- Go 표준 라이브러리만 사용합니다.
- http.Server.Shutdown()을 사용합니다.
- signal.NotifyContext()를 사용합니다.
- sync.WaitGroup으로 Worker 종료를 기다립니다.
- Channel 종료 책임은 Main Goroutine이 가집니다.
- Channel을 닫은 후에는 작업을 송신하지 않아야 합니다.
- 작업 수 집계는 동시성에 안전해야 합니다.
*/

const (
	workerCount = 2
	jobDuration = 500 * time.Millisecond
)

type Job struct {
	ID int64
}

type App struct {
	jobs chan Job

	nextJobID  atomic.Int64
	registered atomic.Int64
	completed  atomic.Int64

	workerWG sync.WaitGroup

	registrationMu sync.RWMutex
	shuttingDown   bool
}

// HTTP 요청으로 작업을 등록한다.
func (a *App) registerJob(w http.ResponseWriter, r *http.Request) {
	// TODO: POST 요청인지 확인한다.
	if r.Method != http.MethodPost {
		w.Header().Set("Allow", http.MethodPost)
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	a.registrationMu.RLock()
	defer a.registrationMu.RUnlock()

	if a.shuttingDown {
		http.Error(w, "server is shutting down", http.StatusServiceUnavailable)
		return
	}

	// TODO: 새로운 작업 ID 를 생성한다.
	jobID := a.nextJobID.Add(1)

	// TODO: Channel 에 작업을 등록한다. 등록 실패 시 적절한 응답을 반환한다.
	select {
	case a.jobs <- Job{ID: jobID}:
		// TODO: 등록 성공 응답을 반환한다.
		a.registered.Add(1)
		w.WriteHeader(http.StatusAccepted)

	default:
		http.Error(w, "job queue is full", http.StatusServiceUnavailable)
	}
}

// Channel 에 저장된 작업을 처리한다.
func (a *App) worker(workerID int) {
	defer a.workerWG.Done()

	for job := range a.jobs {
		// TODO: 작업 시작 로그를 출력한다.
		log.Printf("[Worker%d] Job-%d started\n", workerID, job.ID)

		// TODO: 500ms 동안 작업을 처리한다.
		time.Sleep(jobDuration)

		// TODO: 완료 건수를 증가시킨다.
		a.completed.Add(1)
		log.Printf("[Worker%d] Job-%d completed\n", workerID, job.ID)
	}
}

func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)

	log.Printf("PID: %d\n", os.Getpid())

	app := &App{
		jobs: make(chan Job, 20),
	}

	// TODO: Worker 2개를 시작한다.
	app.workerWG.Add(2)
	for i := range workerCount {
		go func(workerID int) {
			app.worker(workerID)
		}(i + 1)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/jobs", app.registerJob)

	server := &http.Server{
		Addr:    ":8080",
		Handler: mux,
	}

	// TODO: SIGINT/SIGTERM 을 수신할 Context 생성
	signalCtx, stop := signal.NotifyContext(
		context.Background(),
		os.Interrupt,
		syscall.SIGTERM,
	)
	defer stop()

	// TODO: HTTP 서버를 별도 Goroutine 서버에서 실행
	serverDone := make(chan error, 1)
	go func() {
		err := server.ListenAndServe()
		serverDone <- err
	}()
	log.Println("Server started on :8080")

	// TODO: 종료 신호를 기다린다.
	select {
	case <-signalCtx.Done():
		log.Println("Shutdown signal received")

	case err := <-serverDone:
		if !errors.Is(err, http.ErrServerClosed) {
			log.Println("Server failed", err)
		}
	}

	// TODO: 5초 Timeout의 Shutdown Context 생성
	shutdownCtx, cancel := context.WithTimeout(
		context.Background(),
		5*time.Second,
	)
	defer cancel()

	// 새로운 작업 등록 차단
	app.registrationMu.Lock()
	app.shuttingDown = true
	app.registrationMu.Unlock()
	log.Println("New job registration disabled")

	// TODO: HTTP 서버를 먼저 종료하여 신규 작업 등록을 중단한다.
	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Println("Graceful shutdown failed:", err)

		if closeErr := server.Close(); closeErr != nil {
			log.Println("HTTP server close failed:", closeErr)
		}
	}

	// TODO: 작업 Channel을 닫는다.
	// registrationMu 를 통해 모든 송신과 close 동기화
	app.registrationMu.Lock()
	close(app.jobs)
	app.registrationMu.Unlock()
	log.Println("Job channel closed")

	// TODO: Worker가 남은 작업을 모두 처리할 때까지 기다린다.
	workersDone := make(chan struct{})
	go func() {
		app.workerWG.Wait()
		close(workersDone)
	}()

	select {
	case <-workersDone:
		log.Println("All workers completed")

	case <-shutdownCtx.Done():
		log.Println("Worker shutdown timed out")
	}

	registered := app.registered.Load()
	completed := app.completed.Load()
	log.Println("Registered:", registered)
	log.Println("Completed:", completed)

	if registered != completed {
		log.Printf("WARNING: %d jobs were not completed\n", registered-completed)
	} else {
		log.Println("All registered jobs completed")
	}

	log.Println("Shutdown completed")
}

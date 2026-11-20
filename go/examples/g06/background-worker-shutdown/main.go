package main

import (
	"context"
	"log"
	"sync"
	"time"
)

// Worker 는 종료 신호를 받으면 신규 작업을 받지 않는다.
// 이미 시작한 작업은 끝까지 처리한다.
func worker(ctx context.Context, jobs <-chan int, wg *sync.WaitGroup) {
	defer wg.Done()

	for {
		// 작업 수신 또는 종료 신호를 기다린다.
		select {
		case <-ctx.Done():
			log.Println("Worker stopping")
			return

		case job, ok := <-jobs:
			if !ok {
				log.Println("Job channel closed")
				return
			}

			log.Println("Job started:", job)

			// 현재 작업은 종료 신호와 무관하게 완료한다.
			time.Sleep(500 * time.Millisecond)

			log.Println("Job completed:", job)
		}
	}
}

func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)

	ctx, cancel := context.WithCancel(
		context.Background(),
	)
	defer cancel()

	jobs := make(chan int, 10)

	var wg sync.WaitGroup
	wg.Add(1)

	go worker(ctx, jobs, &wg)

	// 작업 5개를 등록한다.
	for i := 1; i <= 5; i++ {
		jobs <- i
	}

	// 첫 번째 작업이 진행되도록 기다린다.
	time.Sleep(200 * time.Millisecond)

	// Worker에 종료 신호를 전달한다.
	cancel()

	// 실제 종료 완료까지 기다린다.
	wg.Wait()

	log.Println("Shutdown completed")
}

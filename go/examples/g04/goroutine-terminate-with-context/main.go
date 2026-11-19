package main

import (
	"context"
	"fmt"
	"runtime"
	"sync"
	"time"
)

// Context 취소 신호를 처리하는 Worker
func worker(ctx context.Context, ch <-chan int, wg *sync.WaitGroup) {
	defer wg.Done()

	select {
	case value := <-ch:
		fmt.Println("Received:", value)

	case <-ctx.Done():
		// Context 가 취소되면 Worker 종료
		return
	}
}

func main() {
	fmt.Println("Before:", runtime.NumGoroutine())

	// 모든 Worker가 공유하는 취소 가능한 Context
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var wg sync.WaitGroup

	for range 1000 {
		wg.Add(1)

		ch := make(chan int)
		go worker(ctx, ch, &wg)
	}

	// Worker들이 Channel 에서 대기하도록 한다.
	time.Sleep(200 * time.Millisecond)

	fmt.Println("After:", runtime.NumGoroutine())

	// 모든 Worker에 취소 신호를 전달한다.
	cancel()

	// Worker가 실제로 종료될 때까지 기다린다.
	wg.Wait()

	fmt.Println("After Cancel:", runtime.NumGoroutine())
}

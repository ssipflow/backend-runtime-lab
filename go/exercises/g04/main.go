package main

import (
	"context"
	"fmt"
	"runtime"
	"sync"
	"time"
)

/**
G-04 문제: 취소 가능한 병렬 작업 처리기
Context · Timeout · Goroutine Lifecycle

10개의 작업을 병렬 처리하는 Worker를 구현합니다.

각 작업은 처리 시간이 서로 다르며, 전체 작업에는 공통 Timeout이 적용됩니다.

요구사항
1. 10개의 작업을 각각 별도의 Goroutine으로 실행합니다.
2. 작업 ID는 1~10입니다.
3. 홀수 ID 작업은 500ms, 짝수 ID 작업은 2초가 걸립니다.
4. 전체 작업에 1초 Timeout을 설정합니다.
5. Timeout 전에 완료한 작업은 성공 결과를 반환합니다.
6. Timeout 전에 완료하지 못한 작업은 취소 결과를 반환합니다.
7. 모든 Goroutine이 종료될 때까지 기다립니다.
8. 작업 시작 전과 종료 후의 Goroutine 수를 출력합니다.

제약조건
- Go 표준 라이브러리만 사용합니다.
- context.WithTimeout()을 사용합니다.
- 각 Worker는 Context 취소 신호를 처리해야 합니다.
- sync.WaitGroup으로 모든 Worker의 종료를 보장합니다.
- Worker가 공유 결과 변수에 직접 쓰지 않도록 합니다.
- 결과 수집에는 Channel을 사용합니다.
*/

const count = 10

type Result struct {
	JobID    int
	Success  bool
	Duration time.Duration
}

// 작업 ID에 따라 실행 시간을 결정한다.
func jobDuration(jobID int) time.Duration {
	if jobID%2 == 1 {
		return 500 * time.Millisecond
	}

	return 2 * time.Second
}

// 개별 작업을 수행한다
func worker(ctx context.Context, jobID int, results chan<- Result, wg *sync.WaitGroup) {
	defer wg.Done()

	result := Result{
		JobID: jobID,
	}

	// TODO: 작업 시작 시간을 기록한다.
	start := time.Now()

	// TODO: 작업 처리 시간과 Context 취소를 select로 동시에 대기한다.
	duration := jobDuration(jobID)
	select {

	case <-time.After(duration):
		result.Duration = duration
		result.Success = true

	case <-ctx.Done():
		result.Duration = time.Since(start)
		result.Success = false
	}

	// TODO: 성공 또는 취소 결과를 Channel 에 전달한다.
	results <- result
}

func main() {
	fmt.Println("Before:", runtime.NumGoroutine())

	// TODO: 1초 Timeout을 갖는 Context 생성
	ctx, cancel := context.WithTimeout(
		context.Background(),
		time.Second,
	)
	defer cancel()

	// TODO: 결과 Channel 을 생성한다.
	results := make(chan Result)

	var wg sync.WaitGroup

	// TODO: 10개의 Worker를 실행한다.
	for i := range 10 {
		wg.Add(1)
		go worker(ctx, i+1, results, &wg)
	}

	// TODO: 모든 Worker가 종료되면 결과 Channel 을 닫는다.
	go func() {
		wg.Wait()
		close(results)
	}()

	// TODO: Channel을 순회하여 결과 집계
	for result := range results {
		fmt.Printf("jobID: %d, duration: %d, success: %t\n",
			result.JobID, result.Duration, result.Success)
	}

	fmt.Println("After:", runtime.NumGoroutine())
}

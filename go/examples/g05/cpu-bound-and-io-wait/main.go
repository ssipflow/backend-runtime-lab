package main

import (
	"fmt"
	"runtime"
	"sync"
	"time"
)

const workerCount = 100

// 고정된 횟수의 CPU 연산 수행
func cpuTask() uint64 {
	var total uint64

	for i := range uint64(2_000_000) {
		total += i
	}

	return total
}

// CPU를 사용하지 않고 일정시간 대기
// 실제 네트워크 I/O를 수행하는 코드는 아니다.
func waitingTask() {
	time.Sleep(100 * time.Millisecond)
}

// CPU-bound 작업을 병렬 실행한다.
func runCPU() {
	var wg sync.WaitGroup
	results := make([]uint64, workerCount)

	start := time.Now()

	for i := range workerCount {
		wg.Add(1)
		go func(index int) {
			defer wg.Done()
			results[index] = cpuTask()
		}(i)
	}
	wg.Wait()

	// 계산 결과를 사용하여 작업이 제거되지 않도록 한다.
	var total uint64
	for _, result := range results {
		total += result
	}

	fmt.Println("CPU total:", total)
	fmt.Println("CPU Elapsed:", time.Since(start))
}

// 대기 작업을 병렬 실행
func runWaiting() {
	var wg sync.WaitGroup
	start := time.Now()

	for range workerCount {
		wg.Go(func() {
			waitingTask()
		})
	}

	wg.Wait()
	fmt.Println("Waiting Elapsed:", time.Since(start))
}

func main() {
	runtime.GOMAXPROCS(1)

	fmt.Println("GOMAXPROCS:", runtime.GOMAXPROCS(0))

	runCPU()
	runWaiting()
}

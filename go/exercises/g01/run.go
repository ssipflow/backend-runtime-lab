package main

import (
	"log"
	"sync"
)

// 순차 실행 구현
func runSequential(fn func(int, *sync.WaitGroup)) {
	for i := 1; i <= 10; i++ {
		fn(i, nil)
	}
	log.Println("All Sequential task ended")
}

// Goroutine 기반 동시 실행 구현
func runConcurrent(fn func(int, *sync.WaitGroup)) {
	var wg sync.WaitGroup

	for i := 1; i <= 10; i++ {
		wg.Add(1)
		go fn(i, &wg)
	}
	wg.Wait()

	log.Println("All Goroutine task ended")
}

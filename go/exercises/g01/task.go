package main

import (
	"log"
	"sync"
	"time"
)

// 하나의 작업을 모사하는 함수
func task(id int, wg *sync.WaitGroup) {
	if wg != nil {
		defer wg.Done()
	}
	log.Printf("Task %d STARTED", id)
	time.Sleep(200 * time.Millisecond)
	log.Printf("Task %d ENDED", id)
}

//func cpuTask(id int, wg *sync.WaitGroup) {
//	if wg != nil {
//		defer wg.Done()
//	}
//
//	// 실제 CPU 연산을 수행하는 작업
//	// 200ms 동안 계산을 반복하며 CPU를 사용
//	deadline := time.Now().Add(200 * time.Millisecond)
//
//	var result uint64
//
//	for time.Now().Before(deadline) {
//		result += uint64(id)
//	}
//
//	log.Printf("CPU Task %d result: %d", id, result)
//}

func cpuTask(id int, wg *sync.WaitGroup) {
	if wg != nil {
		defer wg.Done()
	}

	var result uint64

	// 경과 시간이 아니라 고정된 연산 횟수를 기준으로 실행
	const iterations = 50_000_000

	for i := 0; i < iterations; i++ {
		result += uint64(i) ^ uint64(id)
	}

	log.Printf("CPU Task %d result: %d", id, result)
}

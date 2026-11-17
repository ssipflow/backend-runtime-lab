package main

import (
	"log"
	"runtime"
	"sync"
	"time"
)

func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)

	// 동시에 Go 코드를 실행할 수 있는 P의 개수를 1로 제한
	runtime.GOMAXPROCS(1)

	var wg sync.WaitGroup

	log.Println("CPU:", runtime.NumCPU())
	log.Println("GOMAXPROCS:", runtime.GOMAXPROCS(0))

	for i := 1; i <= 5; i++ {
		wg.Add(1)

		// 각각 독립적인 Goroutine 생성
		go func(id int) {
			defer wg.Done()

			log.Printf("Goroutine %d STARTED\n", id)

			// 현재 Goroutine을 대기시킴
			// 이 시간 동안 다른 Goroutine 이 실행될 수 있음
			time.Sleep(100 * time.Millisecond)

			log.Printf("Goroutine %d ENDED\n", id)
		}(i)
	}

	// 모든 Goroutine이 완료될 때까지 main 대기
	wg.Wait()

	log.Println("All Goroutine ENDED")
}

package main

import (
	"log"
	"sync"
)

func usingMutex() {
	var wg sync.WaitGroup
	counter := &Counter{}

	const workers = 1000
	const iterations = 1000

	for i := 0; i < workers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for j := 0; j < iterations; j++ {
				counter.Increment()
			}
		}()
	}

	// 모든 작업이 완료될 때까지 기다린다.
	wg.Wait()

	log.Println("Expected:", workers*iterations)
	log.Println("Actual:", counter.Value())
}

func usingChannel() {
	const workers = 10

	// 결과를 전달할 Channel 을 생성한다.
	results := make(chan int)

	var wg sync.WaitGroup

	for i := 1; i <= workers; i++ {
		wg.Add(1)

		go func(id int) {
			defer wg.Done()

			// 각 Goroutine은 공유 변수를 수정하지 않고
			// 계산 결과를 Channel로 전달한다.
			results <- id * 10
		}(i)
	}

	// 모든 생산자가 종료되면 Channel 을 닫는다.
	go func() {
		wg.Wait()
		close(results)
	}()

	total := 0

	// Channel 이 닫히고 남은 값이 모두 소비될 때까지
	// 결과를 순차적으로 수신한다.
	for result := range results {
		total += result
	}

	log.Println("Total", total)
}

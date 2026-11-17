package main

/**
G-02 문제: 동시 주문 금액 집계
Mutex와 Channel을 각각 사용하여 동일한 결과를 만드는 실습

주문 1,000건이 존재하며 각 주문의 금액은 100원입니다.
10개의 Worker가 주문을 나누어 처리하고 총 주문 금액을 계산해야 합니다.

요구사항
1. 주문 1,000건을 생성합니다.
2. Worker 10개를 Goroutine으로 실행합니다.
3. 각 Worker는 서로 다른 주문 100건을 처리합니다.
4. runWithMutex()는 공유 total 변수를 Mutex로 보호하여 집계합니다.
5. runWithChannel()은 각 Worker의 계산 결과를 Channel로 전달하여 집계합니다.
6. 두 방식 모두 최종 결과가 100,000원이 되어야 합니다.
제약조건
- Go 표준 라이브러리만 사용합니다.
- sync/atomic 사용은 제외합니다.
- time.Sleep()으로 작업 완료를 기다리면 안 됩니다.
- Mutex 방식과 Channel 방식은 별도 함수로 구현합니다.
*/

import (
	"fmt"
	"log"
	"sync"
)

const (
	orderCount  = 1000
	workerCount = 10
)

type Order struct {
	ID     int
	Amount int
}

// 주문 1,000건 생성
func createOrders() []Order {
	orders := make([]Order, orderCount)

	for i := range orders {
		orders[i] = Order{
			ID:     i + 1,
			Amount: 100,
		}
	}

	return orders
}

// Mutex를 사용하여 주문 금액 집계
func runWithMutex(orders []Order) int {
	var mu sync.Mutex
	var wg sync.WaitGroup
	total := 0

	// TODO:
	// 10개 Worker를 생성합니다.
	// 각 Worker는 서로 다른 100개 주문을 처리합니다.
	// 공유 total을 수정할 때 Mutex를 사용합니다.
	workers := 10
	count := 100
	for i := range workers {
		wg.Add(1)
		go func(workerId int) {
			defer wg.Done()

			subtotal := 0
			for j := range count {
				currentIdx := workerId*count + j
				currentOrder := orders[currentIdx]

				subtotal += currentOrder.Amount
			}

			mu.Lock()
			total += subtotal
			mu.Unlock()
		}(i)
	}

	wg.Wait()
	return total
}

// Channel을 사용하여 주문 금액 집계
func runWithChannel(orders []Order) int {
	results := make(chan int)
	var wg sync.WaitGroup

	// TODO:
	// 10개 Worker를 생성합니다.
	// 각 Worker는 서로 다른 100개 주문을 처리합니다.
	// 각 Worker가 계산한 부분 합계를 results로 전송합니다.
	// 모든 Worker 완료 후 Channel을 닫습니다.
	workers := 10
	count := 100
	for i := range workers {
		wg.Add(1)
		go func(workerId int) {
			defer wg.Done()
			for j := range count {
				currentIdx := workerId*count + j
				currentOrder := orders[currentIdx]
				results <- currentOrder.Amount
			}
		}(i)
	}

	go func() {
		wg.Wait()
		close(results)
	}()

	total := 0

	// TODO:
	// Channel에서 부분 합계를 수신하여 total에 더합니다.
	for result := range results {
		total += result
	}

	return total
}

// Deadlock 발생 예시
// 1. jobs: 용량 10의 Buffered Channel로 생성
// 2. i = 0 부터 0까지 10개의 값이 버퍼에 저장
// 3. i = 10 에서 11번째 값을 전송하려 한다.
// 4. 버퍼가 가득 찼고 수신자가 없으므로 송신 Goroutine Blocking
// 5. 따라서 for 문을 빠져나오지 못하고, 아래에 있는 수신 Goroutine 생성 코드에 도달하지 못함.
func process() {
	jobs := make(chan int, 10)

	for i := range 100 {
		jobs <- i
	}

	go func() {
		for job := range jobs {
			fmt.Println(job)
		}
	}()
}

func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)
	orders := createOrders()

	mutexTotal := runWithMutex(orders)
	channelTotal := runWithChannel(orders)

	fmt.Println("Expected:", 100000)
	fmt.Println("Mutex:", mutexTotal)
	fmt.Println("Channel:", channelTotal)
}

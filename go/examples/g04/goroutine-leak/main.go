package main

import (
	"fmt"
	"runtime"
	"time"
)

// Channel 에서 데이터를 받을 때까지 대기하는 Worker
func worker(ch <-chan int) {
	// 송신자가 없고 Channel 이 닫히지 않으면
	// 이 수신 연산은 무기한 대기한다.
	value := <-ch
	fmt.Println("Received:", value)
}

func main() {
	fmt.Println("Before:", runtime.NumGoroutine())

	// 각 Worker 마다 별도의 Channel을 생성한다.
	// 데이터를 보내거나 Channel을 닫는 코드는 없다.
	for range 1000 {
		ch := make(chan int)
		go worker(ch)
	}

	// Goroutine 들이 실행되어 대기 상태에 진입할 시간을 준다.
	time.Sleep(200 * time.Millisecond)

	fmt.Println("After:", runtime.NumGoroutine())
}

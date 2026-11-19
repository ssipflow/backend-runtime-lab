package main

import (
	"context"
	"fmt"
	"time"
)

// 최대 3초가 걸리는 작업을 가정한다.
func process(ctx context.Context) error {
	select {
	case <-time.After(3 * time.Second):
		// 작업이 정상적으로 완료된 경우
		return nil

	case <-ctx.Done():
		// 취소 또는 Deadline 초과
		return ctx.Err()
	}
}

func main() {
	// 작업에 최대 1초의 시간을 허용한다.
	ctx, cancel := context.WithTimeout(
		context.Background(),
		time.Second,
	)
	defer cancel()

	start := time.Now()
	err := process(ctx)
	fmt.Println("Elapsed:", time.Since(start))
	fmt.Println("Error:", err)
}

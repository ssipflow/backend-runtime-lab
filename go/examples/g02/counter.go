package main

import "sync"

// Counter 는 공유 상태와 이를 보호하는 Mutex를 함께 관리한다.
type Counter struct {
	mu    sync.Mutex
	value int
}

// Increment 는 Mutex를 획득한 후 값을 증가시킨다.
func (c *Counter) Increment() {
	c.mu.Lock()
	// 함수가 종료될 때 반드시 Mutex를 해제한다.
	defer c.mu.Unlock()
	c.value++
}

// Value 는 현재 값을 안전하게 조회한다.
// 읽기 작업도 쓰기 작업과 동기화되어야 한다.
func (c *Counter) Value() int {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.value
}

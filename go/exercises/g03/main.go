package main

import (
	"fmt"
	"runtime"
	"time"
)

/**
G-03 문제: 메모리 할당 방식 비교
Escape Analysis와 Heap Allocation 측정

동일한 데이터 100,000건을 생성하되, 메모리 사용 방식이 서로 다른 두 함수를 구현합니다.

요구사항
1. Payload 구조체는 256바이트의 배열을 포함합니다.
2. runWithValues()는 []Payload에 값을 저장합니다.
3. runWithPointers()는 []*Payload에 포인터를 저장합니다.
4. 각 함수는 100,000개의 Payload를 생성합니다.
5. 두 함수 모두 모든 Payload의 첫 번째 바이트를 합산한 결과를 반환합니다.
6. 각 함수의 실행 시간과 Heap 할당량을 측정합니다.

제약조건
- 표준 라이브러리만 사용합니다.
- 두 함수는 동일한 데이터 생성 규칙을 사용합니다.
- 컴파일러가 작업을 제거하지 않도록 계산 결과를 실제로 사용합니다.
- 측정 중 runtime.GC()를 호출하지 않습니다.
*/

const count = 100_000

type Payload struct {
	Buffer [256]byte
}

// 값 자체를 Slice에 저장합니다.
func runWithValues() int {
	// TODO: []Payload를 생성합니다.
	payloads := make([]Payload, count)

	// TODO: 100,000개 데이터를 저장합니다.
	for i := range count {
		payloads[i].Buffer[0] = byte(i)
	}

	// TODO: Buffer[0]의 합계를 반환합니다.
	total := 0
	for _, payload := range payloads {
		total += int(payload.Buffer[0])
	}

	return total
}

// 포인터를 Slice에 저장합니다.
func runWithPointers() int {
	// TODO: []*Payload를 생성합니다.
	payloads := make([]*Payload, count)

	// TODO: 100,000개 데이터를 저장합니다.
	for i := range count {
		payloads[i] = &Payload{}
		payloads[i].Buffer[0] = byte(i)
	}

	// TODO: Buffer[0]의 합계를 반환합니다.
	total := 0
	for _, payload := range payloads {
		total += int(payload.Buffer[0])
	}

	return total
}

// 실행 시간과 Heap 할당량을 측정합니다.
func measure(label string, fn func() int) {
	runtime.GC()

	var before, after runtime.MemStats
	runtime.ReadMemStats(&before)

	start := time.Now()
	result := fn()
	elapsed := time.Since(start)

	runtime.ReadMemStats(&after)

	fmt.Printf(
		"%s | Result=%d | Time=%s | TotalAlloc=%d KB | Mallocs=%d\n",
		label,
		result,
		elapsed,
		(after.TotalAlloc-before.TotalAlloc)/1024,
		after.Mallocs-before.Mallocs,
	)
}

func main() {
	measure("Values", runWithValues)
	measure("Pointers", runWithPointers)
}

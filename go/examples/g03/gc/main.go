package main

import (
	"fmt"
	"runtime"
)

type Data struct {
	Buffer [1024]byte
}

// Heap 통계 출력
func printMemory(label string) {
	var status runtime.MemStats
	runtime.ReadMemStats(&status)

	fmt.Printf(
		"%s | HeapAlloc=%d KB | HeapObjectes=%d | NumGC=%d\n",
		label,
		status.HeapAlloc/1024,
		status.HeapObjects,
		status.NumGC,
	)
}

func main() {
	printMemory("Before")

	// 각 객체가 서로 다른 메모리를 갖도록 생성한다.
	// 객체를 Slice 에 보관하여 GC가 실행되더라도
	// 접근 가능한 상태로 유지한다.
	objects := make([]*Data, 100_000)

	for i := range objects {
		objects[i] = &Data{}
		objects[i].Buffer[0] = byte(i)
	}

	printMemory("After Allocation")

	// 객체가 실제로 사용된다는 사실을 유지한다.
	runtime.KeepAlive(objects)

	// 이후 객체에 대한 참조를 제거한다.
	objects = nil

	// 관찰 목적으로 GC 를 명시적으로 실행한다.
	runtime.GC()

	printMemory("After GC")
}

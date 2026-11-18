package main

import (
	"fmt"
	"runtime"
)

type Payload struct {
	Value int
}

// 외부에서 참조할 수 있는 Heap 객체 유지
var retained []*Payload

// 지역 변수의 값을 반환한다.
// 포인터를 외부로 전달하지 않는다.
func createLocal() int {
	payload := Payload{Value: 100}
	return payload.Value
}

// 포인터를 반환한다.
// 실제 Heap 할당 여부는 호출 맥락에 따라 달라질 수 있다.
func createPointer() *Payload {
	payload := Payload{Value: 200}
	return &payload
}

// 반환된 포인터를 전역 변수에 저장한다.
// 함수가 종료된 후에도 객체에 접근할 수 있어야 한다.
func retainPointer() {
	payload := createPointer()
	retained = append(retained, payload)
}

func main() {
	// 첫번쨰 함수의 결과를 사용한다.
	local := createLocal()

	// 두번째 함수는 포인터를 반환하지만
	// 호출자에서 값을 즉시 읽기만 한다.
	pointer := createPointer()

	// 세번쨰 함수는 포인터를 전역 상태에 보관한다.
	retainPointer()

	fmt.Println("LOCAL:", local)
	fmt.Println("POINTER:", pointer.Value)
	fmt.Println("REATINED:", retained[0].Value)

	// 현재 Heap 관련 통계 조회
	var status runtime.MemStats
	runtime.ReadMemStats(&status)

	fmt.Println("HeapAlloc", status.HeapAlloc)
	fmt.Println("Mallocs", status.Mallocs)
}

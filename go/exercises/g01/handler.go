package main

import (
	"log"
	"runtime"
	"time"
)

const (
	EXERCISE_TYPE_A       = "A"
	EXERCISE_TYPE_B       = "B"
	EXERCISE_TYPE_C       = "C"
	EXERCISE_TYPE_D       = "D"
	EXERCISE_TYPE_DEFAULT = ""
)

func exerciseHandler(exerciseType string) {
	switch exerciseType {
	case EXERCISE_TYPE_A:
		aExercise()
		break
	case EXERCISE_TYPE_B:
		bExercise()
		break
	case EXERCISE_TYPE_C:
		cExercise()
		break
	case EXERCISE_TYPE_D:
		dExercise()
		break
	default:
		defaultExercise()
		break
	}
	log.Println("Logical CPU:", runtime.NumCPU())
}

/*
*
G-01 문제: 동시 작업 처리 시간 비교
time.Sleep()을 이용해 순차 실행과 Goroutine 기반 동시 실행의 처리 시간을 비교하는 프로그램을 작성하십시오.

요구사항
1. 작업은 총 10개이며, 각 작업은 200ms 동안 대기합니다.
2. runSequential()은 10개 작업을 순차 실행합니다.
3. runConcurrent()는 10개 작업을 각각 Goroutine으로 실행합니다.
4. sync.WaitGroup을 사용해 모든 작업의 완료를 보장합니다.
5. 각 방식의 전체 소요 시간을 출력합니다.

제약조건
- Go 표준 라이브러리만 사용합니다.
- runConcurrent() 내부에서 time.Sleep()으로 전체 완료를 기다리면 안 됩니다.
- GOMAXPROCS(1) 상태에서 실행합니다.
*/
func defaultExercise() {
	runtime.GOMAXPROCS(1)

	start := time.Now()
	runSequential(task)
	log.Println("[DEFAULT] Sequential:", time.Since(start))

	start = time.Now()
	runConcurrent(task)
	log.Println("[DEFAULT] Concurrent:", time.Since(start))
}

// GOMAXPROCS(1)
// runSequential(cpuTask)
func aExercise() {
	runtime.GOMAXPROCS(1)

	start := time.Now()
	runSequential(cpuTask)
	log.Println("[A] Sequential:", time.Since(start))
}

// GOMAXPROCS(1)
// runConcurrent(cpuTask)
func bExercise() {
	runtime.GOMAXPROCS(1)

	start := time.Now()
	runConcurrent(cpuTask)
	log.Println("[B] Concurrent:", time.Since(start))
}

// GOMAXPROCS(2)
// runConcurrent(cpuTask)
func cExercise() {
	runtime.GOMAXPROCS(2)

	start := time.Now()
	runConcurrent(cpuTask)
	log.Println("[C] Concurrent:", time.Since(start))
}

// GOMAXPROCS(4)
// runConcurrent(cpuTask)
func dExercise() {
	runtime.GOMAXPROCS(4)

	start := time.Now()
	runConcurrent(cpuTask)
	log.Println("[D] Concurrent:", time.Since(start))
}

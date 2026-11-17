package main

import "log"

/*
*
Default Exercise
GOMAXPROCS(1)
runSequential(task)
runConcurrent(task)

Exercise A
GOMAXPROCS(1)
runSequential(cpuTask)

Exercise B
GOMAXPROCS(1)
runConcurrent(cpuTask)

Exercise C
GOMAXPROCS(2)
runConcurrent(cpuTask)

Exercise D
GOMAXPROCS(4)
runConcurrent(cpuTask)
*/
func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)

	//exerciseHandler(EXERCISE_TYPE_DEFAULT)
	exerciseHandler(EXERCISE_TYPE_A)
	exerciseHandler(EXERCISE_TYPE_B)
	exerciseHandler(EXERCISE_TYPE_C)
	exerciseHandler(EXERCISE_TYPE_D)
}

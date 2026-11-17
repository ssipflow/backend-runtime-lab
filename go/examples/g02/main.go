package main

import (
	"log"
)

func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)

	exampleHandler(EXAMPLE_MUTEX)
	exampleHandler(EXAMPLE_CHANNEL)
}

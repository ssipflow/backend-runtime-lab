package main

const (
	EXAMPLE_MUTEX   = "MUTEX"
	EXAMPLE_CHANNEL = "CHANNEL"
)

func exampleHandler(exampleType string) {
	switch exampleType {
	case EXAMPLE_MUTEX:
		usingMutex()
		break
	case EXAMPLE_CHANNEL:
		usingChannel()
		break
	}
}

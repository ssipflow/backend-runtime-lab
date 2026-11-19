package main

import (
	"io"
	"log"
	"net/http"
	"sync"
	"sync/atomic"
	"time"
)

const requestCount = 100

func runLoadTest(path string) {
	var wg sync.WaitGroup

	// TODO: 요청 성공 건수를 안전하게 집계한다.
	var successCount atomic.Int64

	start := time.Now()

	baseUrl := "http://localhost:8080"
	url := baseUrl + path
	for range requestCount {

		wg.Go(func() {
			// TODO: HTTP GET 요청을 수행한다.
			// TODO: 응답 본문을 처리하고 닫는다.
			// TODO: 성공한 요청을 집계한다.
			resp, err := http.Get(url)
			if err != nil {
				log.Printf("HTTP request failed: %v", err)
				return
			}
			defer resp.Body.Close()

			//body, err := io.ReadAll(resp.Body)
			//if err != nil {
			//	log.Printf("Failed to read response body: %v", err)
			//}
			//log.Printf("Response: %s", body)

			_, err = io.Copy(io.Discard, resp.Body)
			if err != nil {
				log.Printf("Failed to read response body: %v", err)
				return
			}

			if resp.StatusCode == http.StatusOK {
				successCount.Add(1)
			}
		})
	}

	wg.Wait()

	log.Println("Path:", path)
	log.Println("Elapsed:", time.Since(start))

	// TODO: 성공 건수를 출력한다.
	log.Println("Success:", successCount.Load())
}

func main() {
	log.SetFlags(log.LstdFlags | log.Lmicroseconds)

	runLoadTest("/cpu")
	runLoadTest("/io")
}

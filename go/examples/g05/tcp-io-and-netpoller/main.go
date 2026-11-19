package main

import (
	"fmt"
	"io"
	"net"
	"runtime"
	"sync"
	"time"
)

const connections = 100

func main() {
	// 로컬 TCP 서버 생성
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		panic(err)
	}
	defer listener.Close()

	address := listener.Addr().String()

	var serverWG sync.WaitGroup
	serverWG.Add(connections)

	// 서버는 연결을 수락한 뒤 각각 별도의 Goroutine 에서 읽는다.
	go func() {
		for range connections {
			conn, err := listener.Accept()
			if err != nil {
				return
			}

			go func(c net.Conn) {
				defer serverWG.Done()
				defer c.Close()

				// 클라이언트가 데이터를 보내기 전까지 대기한다.
				// Go Runtime은 네트워크 IO 대기를 효율적으로 관리한다.
				buffer := make([]byte, 1)
				if _, err := io.ReadFull(c, buffer); err != nil {
					return
				}
			}(conn)
		}
	}()

	// 클라이언트 연결을 유지한다.
	clients := make([]net.Conn, 0, connections)
	for range connections {
		conn, err := net.Dial("tcp", address)
		if err != nil {
			panic(err)
		}
		clients = append(clients, conn)
	}

	// 서버의 Read Goroutine들이 대기할 시간을 제공한다.
	time.Sleep(200 * time.Millisecond)

	fmt.Println("Waiting Goroutines:", runtime.NumGoroutine())

	start := time.Now()

	// 대기중인 모든 연결에 데이터를 보낸다.
	for _, conn := range clients {
		if _, err := conn.Write([]byte{1}); err != nil {
			panic(err)
		}
		conn.Close()
	}

	// 서버측 Read가 모두 끝날때까지 기다린다.
	serverWG.Wait()

	fmt.Println("Elapsed:", time.Since(start))
	fmt.Println("After:", runtime.NumGoroutine())
}

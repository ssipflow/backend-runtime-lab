# Backend Runtime Fundamentals Lab

Java, Go, TypeScript/JavaScript의 런타임 내부 동작과 머신 리소스 관리 원리를 학습하기 위한 실습 프로젝트입니다.

프레임워크 중심의 애플리케이션 개발에서 벗어나 **런타임 동작을 직접 구현하고, 측정하고, 분석하는 것**을 목표로 합니다.

## Objectives

- 언어별 런타임과 서버 내부 동작 원리 이해
- CPU, Memory, Thread, I/O 등 머신 리소스 관리 방식 학습
- 동시성, 메모리 누수, GC, 성능 병목 현상 재현
- 프로파일링 도구를 활용한 문제 진단
- 장애 시나리오 기반의 원인 분석 및 해결 능력 강화

## Tech Stack

| Language | Runtime / Environment | Tools |
|---|---|---|
| Java | Java 17+, JDK Standard Library | JFR, jcmd, Thread Dump, Heap Dump |
| Go | Go, Standard Library | pprof, Race Detector |
| TypeScript / JavaScript | Node.js LTS, V8, libuv | Node.js Inspector, CPU / Heap Profiler |

Spring Boot, Express, NestJS 등의 웹 프레임워크는 사용하지 않습니다. 각 언어의 표준 라이브러리를 우선 활용합니다.

## Curriculum

### 1. Java — JVM & Server Internals

`ServerSocket` 기반 HTTP 서버를 직접 구현하며 JVM과 서버 리소스 관리 방식을 학습합니다.

| ID | Topic | Key Concepts |
|---|---|---|
| J-01 | Blocking HTTP Server | ServerSocket, Blocking I/O, Connection |
| J-02 | Thread-per-Connection | Thread Lifecycle, Context Switching |
| J-03 | Thread Pool | ExecutorService, Queue, Rejection |
| J-04 | JVM Memory & GC | Heap, GC, GC Root, Memory Leak |
| J-05 | CPU & I/O | Thread State, CPU Utilization |
| J-06 | Non-blocking I/O | NIO, Selector, Channel |
| J-07 | JVM Profiling | JFR, Thread Dump, Heap Dump |

### 2. Go — Runtime & Concurrency

`net/http` 기반 서버를 통해 Goroutine 스케줄링, 동시성 제어, 메모리 관리와 서버 종료 과정을 학습합니다.

| ID | Topic | Key Concepts |
|---|---|---|
| G-01 | Goroutine Scheduler | GMP, GOMAXPROCS, OS Thread |
| G-02 | Concurrency Control | Channel, Mutex, Race Detector |
| G-03 | Memory & GC | Heap, Escape Analysis, GC |
| G-04 | Goroutine Lifecycle | Context, Cancellation, Leak |
| G-05 | CPU & I/O | Scheduler, netpoller |
| G-06 | Graceful Shutdown | Signal, Server.Shutdown, Timeout |
| G-07 | Profiling | pprof, CPU / Heap / Goroutine Profile |

### 3. TypeScript / JavaScript — Node.js & V8

Node.js 표준 HTTP 모듈을 활용하여 Event Loop, 비동기 I/O, V8 메모리 관리 및 병렬 처리 방식을 학습합니다.

| ID | Topic | Key Concepts |
|---|---|---|
| N-01 | Event Loop | Call Stack, Microtask, Timer |
| N-02 | Event Loop Blocking | CPU-bound, Event Loop Lag |
| N-03 | Asynchronous I/O | libuv, Thread Pool, Non-blocking I/O |
| N-04 | V8 Memory & GC | Heap, GC, Heap Snapshot |
| N-05 | Worker Threads | Parallelism, Message Passing |
| N-06 | HTTP Resource Management | Connection, Timeout, Backpressure |
| N-07 | Shutdown & Profiling | Graceful Shutdown, CPU / Heap Profile |

## Learning Process

각 학습 섹션은 다음 과정으로 진행합니다.

1. **Concept** — 핵심 개념과 런타임 내부 동작 원리 이해
2. **Example** — 샘플 코드 분석 및 실행
3. **Exercise** — 학습한 개념을 활용한 직접 구현
4. **Experiment** — 부하 및 실행 조건을 변경하여 리소스 변화 측정
5. **Review** — 구현과 측정 결과를 분석하고 개선점 도출

### Case Studies

기본 실습 이후 실제 장애 상황을 재현하고 해결하는 시나리오 기반 실습을 진행합니다.

| Language | Scenarios |
|---|---|
| Java | 동시 요청에 따른 응답 지연, Heap 증가와 GC 지연, 낮은 CPU 사용률과 처리량 감소 |
| Go | Goroutine Leak, Race Condition, Graceful Shutdown 중 작업 유실 |
| Node.js | Event Loop Blocking, Memory Leak, 비동기 I/O 처리량 저하 |

Case Study는 다음 순서로 진행합니다.

**Scenario → Hypothesis → Diagnosis → Resolution → Verification**

단순히 정답을 구현하는 것이 아니라, 관찰된 증상으로부터 가설을 수립하고 측정 결과를 근거로 원인을 확인하는 데 집중합니다.

## Learning Principles

- 개념을 암기하기보다 실행 결과를 통해 이해합니다.
- 구현 전 예상 동작을 정의하고 실제 결과와 비교합니다.
- 성능과 리소스 사용량은 가능한 한 측정값을 근거로 판단합니다.
- 문제의 증상과 근본 원인을 구분합니다.
- 수정 전후 동일한 조건에서 재검증합니다.
- 복잡한 비즈니스 로직보다 런타임의 동작 원리에 집중합니다.

## Goal

최종 목표는 Java, Go, Node.js 서버에서 발생하는 **동시성 문제, 메모리 누수, CPU 및 I/O 병목, 비정상적인 응답 지연**을 재현하고, 적절한 진단 도구를 선택하여 원인과 해결 방법을 기술적으로 설명할 수 있는 것입니다.
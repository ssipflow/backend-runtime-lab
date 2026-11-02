#!/bin/bash

# 실행 중인 서버 클래스 이름으로 PID를 자동 조회
PID=$(jps -l | awk '$2 == "j05.CpuWaitExercise" {print $1}')

if [ -z "$PID" ]; then
    echo "서버 프로세스를 찾을 수 없습니다."
    exit 1
fi

echo "PID: $PID"

# CPU 요청 4개를 백그라운드에서 동시 실행
for i in {1..4}; do
    curl -s http://localhost:8080/cpu > /dev/null &
done

# 요청이 실행될 때까지 잠시 대기
sleep 1

# Thread Dump 수집
jcmd "$PID" Thread.print > cpu-thread-dump.txt

# 모든 curl 요청 종료 대기
wait

echo "Thread Dump 저장 완료"

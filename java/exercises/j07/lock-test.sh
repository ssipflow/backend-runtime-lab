#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# JDK 17의 실행 파일 경로를 PATH에 추가합니다.
export PATH="$(/usr/libexec/java_home -v 17)/bin:$PATH"

PID="${1:-$(jps -l | awk '$2 == "j07.DiagnosticExample" {print $1; exit}')}"
if [[ -z "$PID" ]]; then echo 'DiagnosticExample JVM을 찾지 못했습니다.' >&2; exit 1; fi

OUT="results/lock-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
echo "PID=$PID, output=$OUT"

jcmd "$PID" JFR.start name=lock-test settings=profile duration=25s filename="$(pwd)/$OUT/lock-test.jfr"
echo 'JFR이 시작되었습니다. DiagnosticExample 콘솔에서 lock 명령을 입력한 직후 Enter를 누르십시오.'

read -r
jcmd "$PID" Thread.print > "$OUT/lock-thread-dump.txt"
echo 'JFR 기록이 끝날 때까지 27초 대기합니다.'

sleep 27
jfr summary "$OUT/lock-test.jfr" > "$OUT/lock-jfr-summary.txt"
jfr print --events jdk.JavaMonitorEnter "$OUT/lock-test.jfr" > "$OUT/lock-monitor-events.txt"
echo "완료: $OUT"

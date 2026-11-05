#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# JDK 17의 실행 파일 경로를 PATH에 추가합니다.
export PATH="$(/usr/libexec/java_home -v 17)/bin:$PATH"

PID="${1:-$(jps -l | awk '$2 == "j07.DiagnosticExample" {print $1; exit}')}"
if [[ -z "$PID" ]]; then echo 'DiagnosticExample JVM을 찾지 못했습니다.' >&2; exit 1; fi

OUT="results/cpu-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
echo "PID=$PID, output=$OUT"
echo 'DiagnosticExample 콘솔에서 cpu 명령을 입력한 직후 Enter를 누르십시오.'

read -r
jcmd "$PID" Thread.print > "$OUT/cpu-thread-dump.txt"
jcmd "$PID" JFR.start name=cpu-test settings=profile duration=10s filename="$(pwd)/$OUT/cpu-test.jfr"
echo 'JFR 기록 종료까지 12초 대기합니다.'
sleep 12

jfr summary "$OUT/cpu-test.jfr" > "$OUT/cpu-jfr-summary.txt"
jfr print --events jdk.ExecutionSample "$OUT/cpu-test.jfr" > "$OUT/cpu-execution-samples.txt"
echo "완료: $OUT"

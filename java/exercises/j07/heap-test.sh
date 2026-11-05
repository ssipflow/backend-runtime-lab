#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

PID="${1:-$(jps -l | awk '$2 == "j07.DiagnosticExample" {print $1; exit}')}"
if [[ -z "$PID" ]]; then echo 'DiagnosticExample JVM을 찾지 못했습니다.' >&2; exit 1; fi

OUT="results/heap-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"
echo "PID=$PID, output=$OUT"

echo 'DiagnosticExample 콘솔에서 heap 명령을 5회 입력하고 status를 확인한 뒤 Enter를 누르십시오.'
read -r
jcmd "$PID" GC.class_histogram > "$OUT/heap-histogram.txt"
jcmd "$PID" GC.heap_dump "$(pwd)/$OUT/heap-test.hprof"
echo 'DiagnosticExample 콘솔에서 clear 및 status를 실행하고 결과를 비교하십시오.'
echo "완료: $OUT"

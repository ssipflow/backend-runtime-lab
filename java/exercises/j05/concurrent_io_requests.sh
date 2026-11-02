for i in {1..4}; do
  curl -s -o /dev/null \
    -w "Request $i: %{time_total}s\n" \
    http://localhost:8080/io &
done
wait

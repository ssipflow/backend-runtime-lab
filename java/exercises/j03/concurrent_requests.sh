for i in {1..8}; do
  curl -s -o /dev/null \
    -w "Request $i: HTTP %{http_code}, Total %{time_total}s\n" \
    http://localhost:8080/slow &
done
wait
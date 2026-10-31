for i in {1..100}; do
  (
    response=$(curl -s \
      -w "\nHTTP %{http_code}, Total %{time_total}s" \
      http://localhost:8080/status)

    printf 'Request %d:\n%s\n--------------------\n' "$i" "$response"
  ) &
done
wait
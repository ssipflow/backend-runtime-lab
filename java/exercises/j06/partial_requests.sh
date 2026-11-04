python3 - <<'PY'
import socket
import time

with socket.create_connection(("localhost", 8080)) as s:
    s.sendall(b"GET /fast HTTP/1.1\r\n")
    time.sleep(1)
    s.sendall(b"Host: localhost\r\n")
    time.sleep(1)
    s.sendall(b"\r\n")

    print(s.recv(4096).decode())
PY
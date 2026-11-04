python3 - <<'PY'
import socket
import time

clients = []

for _ in range(100):
    client = socket.create_connection(("localhost", 8080))
    clients.append(client)

print("100 connections established")
time.sleep(30)

for client in clients:
    client.close()
PY
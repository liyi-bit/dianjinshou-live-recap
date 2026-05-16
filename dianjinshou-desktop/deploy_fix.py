#!/usr/bin/env python3
"""Upload fixed file and rebuild."""

import paramiko
import os
import time
import socket

HOST = os.environ["DIANJINSHOU_DEPLOY_HOST"]
USER = os.environ.get("DIANJINSHOU_DEPLOY_USER", "root")
PASSWORD = os.environ["DIANJINSHOU_DEPLOY_PASSWORD"]

def get_ssh():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    return ssh

# Upload fixed file
print("=== Uploading fixed AnalysisTaskProcessor.java ===")
ssh = get_ssh()
sftp = ssh.open_sftp()
local = r"D:\download\cursorProject\dianjinshou\dianjinshou-server\src\main\java\com\dianjinshou\modules\recap\task\AnalysisTaskProcessor.java"
remote = "/opt/dianjinshou/dianjinshou-server/src/main/java/com/dianjinshou/modules/recap/task/AnalysisTaskProcessor.java"
sftp.put(local, remote)
print(f"Uploaded ({sftp.stat(remote).st_size} bytes)")
sftp.close()

# Clean status file and start build
print("\n=== Starting rebuild ===")
channel = ssh.get_transport().open_session()
channel.exec_command(
    "rm -f /tmp/djsh-deploy-status && "
    "nohup bash -c '"
    "cd /opt/dianjinshou && "
    "docker compose -f docker-compose.prod.yml build --no-cache server && "
    "docker compose -f docker-compose.prod.yml up -d server && "
    "echo SUCCESS > /tmp/djsh-deploy-status || "
    "echo FAILED > /tmp/djsh-deploy-status"
    "' > /tmp/djsh-deploy.log 2>&1 &"
)
channel.settimeout(5)
try:
    channel.recv(4096)
except (socket.timeout, Exception):
    pass
ssh.close()
print("Build started in background.")

# Poll
print("Polling (every 15s, up to 10 min)...")
for i in range(40):
    time.sleep(15)
    elapsed = (i + 1) * 15
    try:
        s = get_ssh()
        _, out, _ = s.exec_command("cat /tmp/djsh-deploy-status 2>/dev/null || echo BUILDING", timeout=10)
        status = out.read().decode().strip()
        _, out2, _ = s.exec_command("tail -2 /tmp/djsh-deploy.log 2>/dev/null", timeout=10)
        last = out2.read().decode().strip().replace('\n', ' | ')
        s.close()
        print(f"  [{elapsed}s] {status} | {last[:150]}")
        if status in ("SUCCESS", "FAILED"):
            break
    except Exception as e:
        print(f"  [{elapsed}s] Error: {e}")

# Show log
print("\n=== Build log (last 50 lines) ===")
s = get_ssh()
_, out, _ = s.exec_command("tail -50 /tmp/djsh-deploy.log", timeout=15)
print(out.read().decode())

_, out, _ = s.exec_command("cat /tmp/djsh-deploy-status 2>/dev/null || echo UNKNOWN", timeout=10)
final = out.read().decode().strip()
print(f"Build status: {final}")

if final != "SUCCESS":
    print("Build failed!")
    s.close()
    exit(1)

# Wait for startup
print("\n=== Waiting 25s for Spring Boot startup ===")
time.sleep(25)

print("\n=== Container logs (last 60 lines) ===")
_, out, _ = s.exec_command(
    "cd /opt/dianjinshou && docker compose -f docker-compose.prod.yml logs --tail=60 server 2>&1",
    timeout=30,
)
logs = out.read().decode()
print(logs)

print("\n=== Container status ===")
_, out, _ = s.exec_command(
    "cd /opt/dianjinshou && docker compose -f docker-compose.prod.yml ps server 2>&1",
    timeout=15,
)
print(out.read().decode())
s.close()

if "Started" in logs or "JVM running" in logs or "Tomcat started" in logs:
    print("SUCCESS: Server started!")
elif "ERROR" in logs or "Exception" in logs:
    print("WARNING: Errors in logs")
else:
    print("INFO: Check manually")
print("Done.")

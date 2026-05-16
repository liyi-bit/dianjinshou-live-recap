#!/usr/bin/env python3
"""Build and deploy dianjinshou-server (files already uploaded)."""

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

# Step 1: Start build in background
print("=== Starting build in background on server ===")
ssh = get_ssh()
# Use nohup + redirect, get PID via a marker file
start_cmd = (
    "nohup bash -c '"
    "cd /opt/dianjinshou && "
    "docker compose -f docker-compose.prod.yml build --no-cache server && "
    "docker compose -f docker-compose.prod.yml up -d server && "
    "echo SUCCESS > /tmp/djsh-deploy-status || "
    "echo FAILED > /tmp/djsh-deploy-status"
    "' > /tmp/djsh-deploy.log 2>&1 &\n"
    "echo $! > /tmp/djsh-deploy-pid && cat /tmp/djsh-deploy-pid"
)
# Use invoke_shell for more reliable background process handling
channel = ssh.get_transport().open_session()
channel.exec_command(start_cmd)
channel.settimeout(30)
try:
    output = b""
    while True:
        try:
            chunk = channel.recv(4096)
            if not chunk:
                break
            output += chunk
        except socket.timeout:
            break
    pid = output.decode().strip().split('\n')[-1].strip()
    print(f"Build PID: {pid}")
except Exception as e:
    print(f"Note: {e}")
    pid = "unknown"
ssh.close()

# Step 2: Poll for completion with fresh connections
print("Polling for build completion (checking every 15s, up to 10 min)...")
for i in range(40):
    time.sleep(15)
    elapsed = (i + 1) * 15
    try:
        ssh2 = get_ssh()
        _, stdout, _ = ssh2.exec_command("cat /tmp/djsh-deploy-status 2>/dev/null || echo BUILDING", timeout=10)
        status = stdout.read().decode().strip()
        _, stdout2, _ = ssh2.exec_command("tail -2 /tmp/djsh-deploy.log 2>/dev/null", timeout=10)
        last = stdout2.read().decode().strip().replace('\n', ' | ')
        ssh2.close()
        print(f"  [{elapsed}s] {status} | {last[:120]}")
        if status in ("SUCCESS", "FAILED"):
            break
    except Exception as e:
        print(f"  [{elapsed}s] Connection error: {e}")

# Step 3: Show results
print("\n=== Build log (last 40 lines) ===")
ssh3 = get_ssh()
_, stdout, _ = ssh3.exec_command("tail -40 /tmp/djsh-deploy.log", timeout=15)
print(stdout.read().decode())

# Check final status
_, stdout, _ = ssh3.exec_command("cat /tmp/djsh-deploy-status 2>/dev/null || echo UNKNOWN", timeout=10)
final_status = stdout.read().decode().strip()
print(f"Build status: {final_status}")

if final_status != "SUCCESS":
    print("ERROR: Build or deploy failed. Check /tmp/djsh-deploy.log on server.")
    ssh3.close()
    exit(1)

# Step 4: Wait for Spring Boot startup
print("\n=== Waiting 25 seconds for Spring Boot startup ===")
time.sleep(25)

# Step 5: Check container logs
print("\n=== Container logs (last 60 lines) ===")
_, stdout, _ = ssh3.exec_command(
    "cd /opt/dianjinshou && docker compose -f docker-compose.prod.yml logs --tail=60 server 2>&1",
    timeout=30,
)
logs = stdout.read().decode()
print(logs)

# Container status
print("\n=== Container status ===")
_, stdout, _ = ssh3.exec_command(
    "cd /opt/dianjinshou && docker compose -f docker-compose.prod.yml ps server 2>&1",
    timeout=15,
)
print(stdout.read().decode())

ssh3.close()

if "Started" in logs or "JVM running" in logs or "Tomcat started" in logs:
    print("SUCCESS: Server started successfully!")
elif "ERROR" in logs or "Exception" in logs:
    print("WARNING: Errors found in logs")
else:
    print("INFO: Verify startup manually")

print("Done.")

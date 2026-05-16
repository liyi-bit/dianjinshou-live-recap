#!/usr/bin/env python3
"""Deploy updated dianjinshou-server files to remote server."""

import paramiko
import os
import sys
import time

# Server config
HOST = os.environ["DIANJINSHOU_DEPLOY_HOST"]
USER = os.environ.get("DIANJINSHOU_DEPLOY_USER", "root")
PASSWORD = os.environ["DIANJINSHOU_DEPLOY_PASSWORD"]
REMOTE_BASE = os.environ.get("DIANJINSHOU_REMOTE_BASE", "/opt/dianjinshou/dianjinshou-server")
LOCAL_BASE = os.environ.get("DIANJINSHOU_LOCAL_BASE", os.path.abspath("../dianjinshou-server"))

# Files to upload (relative paths)
FILES = [
    "src/main/java/com/dianjinshou/modules/recap/dto/SubmitAsrResultRequest.java",
    "src/main/java/com/dianjinshou/modules/settings/controller/SettingsController.java",
    "src/main/java/com/dianjinshou/modules/recap/service/AnalysisService.java",
    "src/main/java/com/dianjinshou/modules/recap/task/AnalysisTaskProcessor.java",
    "src/main/java/com/dianjinshou/modules/recap/controller/AnalysisController.java",
    "src/main/java/com/dianjinshou/modules/recording/service/RecordingService.java",
    "src/main/java/com/dianjinshou/modules/recording/controller/RecordingController.java",
    "src/main/java/com/dianjinshou/modules/recording/entity/Recording.java",
    "src/main/resources/db/migration/V16__add_clip_file_path.sql",
    "src/main/resources/db/migration/V17__add_recording_storage_key.sql",
    "Dockerfile",
]


def main():
    # Verify local files exist
    print("=== Verifying local files ===")
    missing = []
    for f in FILES:
        local_path = os.path.join(LOCAL_BASE, f.replace("/", os.sep))
        if not os.path.isfile(local_path):
            missing.append(f)
            print(f"  MISSING: {f}")
        else:
            size = os.path.getsize(local_path)
            print(f"  OK: {f} ({size} bytes)")
    if missing:
        print(f"\nERROR: {len(missing)} files missing. Aborting.")
        sys.exit(1)

    # Connect
    print(f"\n=== Connecting to {HOST} ===")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    sftp = ssh.open_sftp()
    print("Connected.")

    # Upload files
    print("\n=== Uploading files ===")
    for f in FILES:
        local_path = os.path.join(LOCAL_BASE, f.replace("/", os.sep))
        remote_path = f"{REMOTE_BASE}/{f}"
        remote_dir = os.path.dirname(remote_path).replace("\\", "/")

        # Ensure remote directory exists
        _stdin, _stdout, _stderr = ssh.exec_command(f"mkdir -p {remote_dir}")
        _stdout.channel.recv_exit_status()

        sftp.put(local_path, remote_path)
        remote_size = sftp.stat(remote_path).st_size
        print(f"  Uploaded: {f} ({remote_size} bytes)")

    sftp.close()
    print("All files uploaded.")

    # Detect docker compose command
    print("\n=== Detecting docker compose command ===")
    stdin, stdout, stderr = ssh.exec_command("which docker-compose 2>/dev/null || docker compose version 2>/dev/null", timeout=10)
    detect_out = stdout.read().decode().strip()
    print(f"  Detection: {detect_out}")
    # Use 'docker compose' (v2) as fallback
    dc = "docker compose" if "docker-compose" not in detect_out or "not found" in detect_out else "docker-compose"
    print(f"  Using: {dc}")

    # Build and deploy
    print("\n=== Building and deploying (this takes 3-5 minutes) ===")
    build_cmd = (
        f"cd /opt/dianjinshou && "
        f"{dc} -f docker-compose.prod.yml build --no-cache server 2>&1 && "
        f"{dc} -f docker-compose.prod.yml up -d server 2>&1"
    )
    stdin, stdout, stderr = ssh.exec_command(build_cmd, timeout=600)

    # Stream output
    for line in iter(stdout.readline, ""):
        print(f"  {line}", end="")
    exit_code = stdout.channel.recv_exit_status()
    err = stderr.read().decode()
    if err:
        print(f"  STDERR: {err}")

    if exit_code != 0:
        print(f"\nERROR: Build/deploy failed with exit code {exit_code}")
        ssh.close()
        sys.exit(1)

    print("\nBuild and deploy completed.")

    # Wait a moment for container to start
    print("\n=== Waiting 15 seconds for container startup ===")
    time.sleep(15)

    # Check logs
    print("\n=== Checking container logs ===")
    stdin, stdout, stderr = ssh.exec_command(
        f"cd /opt/dianjinshou && {dc} -f docker-compose.prod.yml logs --tail=80 server 2>&1",
        timeout=30,
    )
    logs = stdout.read().decode()
    print(logs)

    # Check if started successfully
    if "Started" in logs or "JVM running" in logs or "Tomcat started" in logs:
        print("\n=== SUCCESS: Server started successfully ===")
    elif "ERROR" in logs or "Exception" in logs:
        print("\n=== WARNING: Errors found in logs, please review ===")
    else:
        print("\n=== INFO: Could not determine startup status from logs, please verify manually ===")

    # Also check container status
    stdin, stdout, stderr = ssh.exec_command(
        f"cd /opt/dianjinshou && {dc} -f docker-compose.prod.yml ps server 2>&1",
        timeout=15,
    )
    ps_output = stdout.read().decode()
    print(f"\nContainer status:\n{ps_output}")

    ssh.close()
    print("Done.")


if __name__ == "__main__":
    main()

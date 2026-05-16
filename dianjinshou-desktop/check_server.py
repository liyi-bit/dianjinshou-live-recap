import paramiko
import os
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(
    os.environ["DIANJINSHOU_DEPLOY_HOST"],
    username=os.environ.get("DIANJINSHOU_DEPLOY_USER", "root"),
    password=os.environ["DIANJINSHOU_DEPLOY_PASSWORD"],
    timeout=30,
)

cmds = [
    "ls -la /opt/dianjinshou/",
    "ls -la /opt/dianjinshou/*.yml /opt/dianjinshou/*.yaml 2>/dev/null",
    "cat /opt/dianjinshou/docker-compose.yml 2>/dev/null || cat /opt/dianjinshou/compose.yml 2>/dev/null || echo 'No compose file found'",
    "docker compose ls 2>/dev/null",
]
for cmd in cmds:
    print(f"\n=== {cmd} ===")
    _, stdout, stderr = ssh.exec_command(cmd, timeout=15)
    print(stdout.read().decode())
    err = stderr.read().decode()
    if err:
        print(f"STDERR: {err}")
ssh.close()

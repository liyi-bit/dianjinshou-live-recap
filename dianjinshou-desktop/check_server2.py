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
    "cat /opt/dianjinshou/docker-compose.prod.yml",
    "ls -la /opt/dianjinshou/dianjinshou-server/",
    "ls -la /opt/dianjinshou/dianjinshou-server/src/main/java/com/dianjinshou/modules/recap/dto/ 2>/dev/null || echo 'dto dir not found'",
    "ls -la /opt/dianjinshou/dianjinshou-server/Dockerfile 2>/dev/null || echo 'No Dockerfile in server dir'",
]
for cmd in cmds:
    print(f"\n=== {cmd} ===")
    _, stdout, stderr = ssh.exec_command(cmd, timeout=15)
    print(stdout.read().decode())
ssh.close()

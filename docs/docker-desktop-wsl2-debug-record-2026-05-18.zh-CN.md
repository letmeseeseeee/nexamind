# Docker Desktop WSL2 启动问题调试记录

日期：2026-05-18 至 2026-05-19

项目路径：`F:\ai_agent\AgentX`

GitHub 仓库：`https://github.com/lucky-aeon/AgentX`

## 问题概述

Docker Desktop 可以打开界面，也可以登录账号，但 Docker Engine 一直卡在：

```text
Starting the Docker Engine...
Docker Engine is the underlying technology that runs containers
```

命令行表现为 Docker CLI 能运行，但无法连接到 Docker Desktop Linux Engine，`docker info` 返回 500。

这个问题分两轮出现：

- 第一轮根因：Windows Hypervisor 启动项被关闭，导致 WSL2 后端不可用。
- 第二轮根因：Docker 数据迁移到 `F:` 盘后，WSL2 挂载 Docker VHDX 虚拟磁盘时被 Windows 拒绝访问；同时系统 PATH 中存在无效路径 `D:\matlab\bin`，干扰 WSL bootstrap。

最终 Docker Engine 已恢复正常，`docker run --rm hello-world` 验证通过。

## 用户目标

- 将 AgentX 项目确认上传到 GitHub。
- 修复本机 Docker Desktop，保证能用于 AgentX 的 Docker 开发。
- 尽量不要占用 `C:` 盘，把 Docker 镜像、容器、卷等大文件迁移到 `F:` 盘。
- 保留完整调试记录。

## GitHub 状态检查

执行命令：

```powershell
git status --short --branch
git remote -v
git log --oneline --decorate -5
```

观察结果：

```text
## master...origin/master

origin  https://github.com/lucky-aeon/AgentX.git (fetch)
origin  https://github.com/lucky-aeon/AgentX.git (push)

3ee7b0b6 (HEAD -> master, origin/master, origin/HEAD) fix: 前端markdown乱码问题
```

说明：

- 本地分支为 `master`。
- 远端为 `https://github.com/lucky-aeon/AgentX.git`。
- 本地 `HEAD` 与 `origin/master` 指向同一个提交。

尝试执行：

```powershell
git push origin master
git ls-remote --heads origin master
```

出现过 GitHub HTTPS 连接重置或超时：

```text
OpenSSL SSL_read: Connection was reset, errno 10054
Failed to connect to github.com port 443
```

再次检查本地提交：

```powershell
git rev-parse HEAD
git rev-parse origin/master
```

两者一致：

```text
3ee7b0b6cee98377185e3b1135f5400221cddf16
```

结论：项目已经绑定到 GitHub，且当前没有未上传提交。`git push` 失败属于当前 Git HTTPS 网络传输问题，不是仓库状态问题。

## 第一轮 Docker 故障现象

执行：

```powershell
docker version
docker compose version
docker context ls
docker desktop status
docker info
```

观察到：

```text
Docker Compose version v5.1.3
```

说明 Docker CLI 和 Compose 插件存在。

但 Docker Desktop 状态一直是：

```text
Status starting
```

`docker info` 报错：

```text
request returned 500 Internal Server Error for API route and version
http://%2F%2F.%2Fpipe%2FdockerDesktopLinuxEngine/v1.54/info
```

说明 Docker Desktop 前端可以运行，但 Linux Engine 后端没有正常启动。

## WSL 和 Windows 功能检查

执行：

```powershell
Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux
Get-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform
wsl --status
wsl --list --verbose
```

观察到：

- `Microsoft-Windows-Subsystem-Linux` 已启用。
- `VirtualMachinePlatform` 已启用。
- 但 Docker 的 WSL 后端没有正常工作。

Docker 日志中出现：

```text
WSL_E_WSL2_NEEDED
WSL_E_DISTRO_NOT_FOUND
```

说明 Docker Desktop 尝试创建或启动 `docker-desktop` WSL 发行版失败。

## 第一轮根因：Hypervisor 被关闭

执行：

```powershell
bcdedit /enum '{current}' | Select-String -Pattern 'hypervisorlaunchtype'
```

观察到：

```text
hypervisorlaunchtype    Off
```

这会导致 WSL2 后端无法正常工作。Docker Desktop 在 Windows Home 上依赖 WSL2，因此即使 Docker Desktop UI 能打开，Engine 也会卡在启动阶段。

## 第一轮修复：开启 Hypervisor

执行：

```powershell
bcdedit /set hypervisorlaunchtype auto
```

结果：

```text
The operation completed successfully.
```

重启 Windows 后验证：

```powershell
bcdedit /enum '{current}' | Select-String -Pattern 'hypervisorlaunchtype'
Get-CimInstance Win32_ComputerSystem | Select-Object HypervisorPresent
```

结果：

```text
hypervisorlaunchtype    Auto
HypervisorPresent       True
```

说明 Hypervisor 已经正常启用。

## 将 Docker 数据迁移到 F 盘

用户要求尽量不要占用 `C:` 盘。因此将 Docker Desktop 的 WSL 数据目录迁移到：

```text
F:\DockerDesktop\wsl
```

创建目录：

```powershell
New-Item -ItemType Directory -Force -Path "F:\DockerDesktop\wsl"
New-Item -ItemType Directory -Force -Path "F:\DockerDesktop\vm-data"
```

备份 Docker Desktop 设置：

```powershell
Copy-Item "$env:APPDATA\Docker\settings-store.json" "$env:APPDATA\Docker\settings-store.json.bak-时间戳"
```

设置文件路径：

```text
C:\Users\怎样都能钩中你\AppData\Roaming\Docker\settings-store.json
```

关键配置：

```json
{
  "CustomWslDistroDir": "F:\\DockerDesktop\\wsl",
  "DataFolder": "F:\\DockerDesktop\\vm-data",
  "UseContainerdSnapshotter": false
}
```

说明：Docker Desktop 日志确认实际读取到了 `CustomWslDistroDir` 和 `DataFolder`，并将 `wslDataFolder` 指向 `F:\DockerDesktop\wsl`。

## 停止卡住的 Docker 进程

当 `docker desktop stop` 自身也卡住时，手动结束 Docker 相关进程：

```powershell
$procs = Get-Process | Where-Object {
  $_.ProcessName -like '*Docker*' -or
  $_.ProcessName -like 'com.docker*' -or
  $_.ProcessName -eq 'docker'
}
$procs | Stop-Process -Force -ErrorAction SilentlyContinue
wsl --shutdown
```

涉及进程包括：

```text
Docker Desktop
com.docker.backend
com.docker.build
docker
docker-desktop
docker-agent
docker-sandbox
```

## 处理 C 盘残留

迁移后，检查 `C:` 盘 Docker 残留：

```powershell
Test-Path "$env:LOCALAPPDATA\Docker\wsl"
Test-Path "$env:LOCALAPPDATA\Docker\wsl.migrated-to-F"
```

确认旧的 Docker WSL 数据不再位于：

```text
C:\Users\怎样都能钩中你\AppData\Local\Docker\wsl
```

清理了安全残留：

- 空目录：`C:\Users\怎样都能钩中你\AppData\Local\Docker\wsl.migrated-to-F`
- 调试备份：`C:\Users\怎样都能钩中你\AppData\Roaming\Docker\settings-store.json.bak-*`

保留了 Docker 正常运行可能需要的文件：

- `C:\Users\怎样都能钩中你\AppData\Local\Docker\log`
- `C:\Users\怎样都能钩中你\AppData\Local\Docker\run`
- `C:\Users\怎样都能钩中你\AppData\Local\Docker\tasks`
- `C:\Users\怎样都能钩中你\AppData\Roaming\Docker\settings-store.json`
- Docker Desktop 程序本体

清理后，Docker 在 `C:` 盘只剩少量配置和日志，真正的大文件位于：

```text
F:\DockerDesktop\wsl
```

## 第一轮验证结果

执行：

```powershell
docker desktop start
docker desktop status
docker info
wsl --list --verbose
docker run --rm hello-world
```

结果：

```text
Status running
Server Version: 29.4.2
docker-desktop Running 2
Hello from Docker!
```

此时 Docker Engine 正常。

## 第二轮故障：再次卡在 Starting Docker Engine

第二天再次出现：

```text
Starting the Docker Engine...
Docker Engine is the underlying technology that runs containers
```

执行：

```powershell
docker desktop status
docker info
wsl --list --verbose
```

观察到：

```text
Status starting
```

`docker info` 仍然返回 500：

```text
request returned 500 Internal Server Error
```

WSL 状态：

```text
docker-desktop    Stopped    2
```

说明 Docker Desktop 前端启动了，但 `docker-desktop` WSL 后端没有稳定运行。

## 第二轮排查：确认不是 Hypervisor 复发

再次检查：

```powershell
bcdedit /enum '{current}' | Select-String -Pattern 'hypervisorlaunchtype'
Get-CimInstance Win32_ComputerSystem | Select-Object HypervisorPresent
```

结果仍然正常：

```text
hypervisorlaunchtype    Auto
HypervisorPresent       True
```

说明这次不是 Hypervisor 被关闭。

## 第二轮根因 1：F 盘 VHDX 挂载被拒绝访问

Docker 日志中出现明确错误：

```text
无法将磁盘 "\\?\F:\DockerDesktop\wsl\main\ext4.vhdx" 附加到 WSL2: 拒绝访问。
错误代码: Wsl/Service/CreateInstance/MountDisk/HCS/E_ACCESSDENIED
```

以及：

```text
无法将磁盘 "\\?\F:\DockerDesktop\wsl\disk\docker_data.vhdx" 附加到 WSL2: 拒绝访问。
错误代码: Wsl/Service/AttachDisk/MountDisk/HCS/E_ACCESSDENIED
```

说明 Docker Desktop 读取到了 `F:\DockerDesktop\wsl`，但 WSL2/HCS 在挂载 VHDX 时权限不足。

检查文件：

```powershell
Get-Acl 'F:\DockerDesktop\wsl\main\ext4.vhdx','F:\DockerDesktop\wsl\disk\docker_data.vhdx'
Get-Item 'F:\DockerDesktop\wsl\main\ext4.vhdx','F:\DockerDesktop\wsl\disk\docker_data.vhdx'
```

文件存在，但需要补充明确权限。

## 第二轮根因 2：系统 PATH 中存在无效 D 盘路径

手动启动 WSL 时出现：

```text
wsl: Failed to translate 'D:\matlab\bin'
```

检查路径：

```powershell
Test-Path 'D:\matlab\bin'
[Environment]::GetEnvironmentVariable('Path','Machine') -split ';' | Where-Object { $_ -match 'matlab|^D:' }
```

结果：

```text
False
D:\matlab\bin
```

说明机器级 PATH 中存在一个已经不存在的路径。WSL 启动时会尝试翻译 Windows PATH，这个无效项会干扰 Docker Desktop 的 WSL bootstrap。

## 第二轮修复步骤

先关闭 Docker 和 WSL：

```powershell
Get-Process | Where-Object {
  $_.ProcessName -like '*Docker*' -or
  $_.ProcessName -like 'com.docker*' -or
  $_.ProcessName -eq 'docker'
} | Stop-Process -Force -ErrorAction SilentlyContinue

wsl --shutdown
```

给 Docker 的 `F:` 盘数据目录补明确权限：

```powershell
$currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$userGrant = $currentUser + ':(OI)(CI)F'

icacls 'F:\DockerDesktop' /grant $userGrant 'SYSTEM:(OI)(CI)F' 'Administrators:(OI)(CI)F' /T /C
```

处理结果：

```text
Successfully processed 12 files; Failed processing 0 files
```

从机器 PATH 中删除不存在的路径：

```powershell
$machinePath = [Environment]::GetEnvironmentVariable('Path','Machine')
$parts = $machinePath -split ';' | Where-Object { $_ -and ($_ -ne 'D:\matlab\bin') }
$newMachinePath = ($parts -join ';')
[Environment]::SetEnvironmentVariable('Path', $newMachinePath, 'Machine')
```

同时清理当前 PowerShell 会话中的 PATH 残留：

```powershell
$env:Path = (($env:Path -split ';' | Where-Object { $_ -and ($_ -ne 'D:\matlab\bin') }) -join ';')
```

重新启动 Docker：

```powershell
docker desktop start
```

结果：

```text
✓ Starting Docker Desktop
```

## 第二轮验证结果

执行：

```powershell
docker desktop status
docker info --format "ServerVersion={{.ServerVersion}} DockerRootDir={{.DockerRootDir}} OperatingSystem={{.OperatingSystem}}"
wsl --list --verbose
docker image ls hello-world
docker run --rm hello-world
```

结果：

```text
Status running
ServerVersion=29.4.2 DockerRootDir=/var/lib/docker OperatingSystem=Docker Desktop
docker-desktop Running 2
```

容器执行成功：

```text
Hello from Docker!
This message shows that your installation appears to be working correctly.
```

## 当前最终状态

- Docker Desktop：`running`
- Docker Engine：正常
- Docker Server Version：`29.4.2`
- WSL 发行版：`docker-desktop Running 2`
- Docker 数据目录：`F:\DockerDesktop\wsl`
- Docker 数据盘：`F:\DockerDesktop\wsl\disk\docker_data.vhdx`
- Docker 主发行版盘：`F:\DockerDesktop\wsl\main\ext4.vhdx`
- `D:\matlab\bin`：已从机器 PATH 中移除
- `F:\DockerDesktop`：已补充当前用户、SYSTEM、Administrators 完全控制权限
- `hello-world` 容器测试：通过

## AgentX Docker Compose 校验

执行：

```powershell
cd F:\ai_agent\AgentX\deploy
docker compose --env-file .env -f docker-compose.yml --profile local config --quiet
```

结果：无输出，退出码为 0，表示 Compose 配置解析通过。

## 启动 AgentX

```powershell
cd F:\ai_agent\AgentX\deploy
.\start.bat
```

## 后续排障建议

如果 Docker Desktop 再次卡在 `Starting the Docker Engine...`，优先按这个顺序检查：

```powershell
docker desktop status
docker info
wsl --list --verbose
bcdedit /enum '{current}' | Select-String -Pattern 'hypervisorlaunchtype'
Get-CimInstance Win32_ComputerSystem | Select-Object HypervisorPresent
```

然后检查 Docker 日志中是否出现：

```text
E_ACCESSDENIED
Failed to translate
WSL_E_WSL2_NEEDED
context deadline exceeded
```

重点关注：

- `F:\DockerDesktop\wsl\main\ext4.vhdx`
- `F:\DockerDesktop\wsl\disk\docker_data.vhdx`
- 机器 PATH 中是否有不存在的盘符路径
- `hypervisorlaunchtype` 是否仍为 `Auto`

经验结论：

- Docker Desktop 能登录账号，不代表 Docker Engine 正常。
- `Starting the Docker Engine...` 通常要看 WSL 和 VHDX 挂载日志。
- Docker 数据迁移到非 `C:` 盘后，要特别注意 VHDX 权限。
- WSL 会翻译 Windows PATH，坏的系统 PATH 项可能导致 Docker Desktop bootstrap 失败。

param(
    [int]$FrontendPort = 3000,
    [int]$BackendPort = 8088
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$localRoot = Join-Path $projectRoot ".local"
$logRoot = Join-Path $localRoot "logs"
$tempRoot = Join-Path $localRoot "temp"
$mavenRepository = Join-Path $localRoot "maven-repository"
$npmCache = Join-Path $localRoot "npm-cache"

@($localRoot, $logRoot, $tempRoot, $mavenRepository, $npmCache) | ForEach-Object {
    New-Item -ItemType Directory -Path $_ -Force | Out-Null
}

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        if ($line -notmatch '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
            continue
        }

        $name = $Matches[1]
        $value = $Matches[2].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

Import-DotEnv (Join-Path $PSScriptRoot ".env")
Import-DotEnv (Join-Path $projectRoot ".env")

$env:TEMP = $tempRoot
$env:TMP = $tempRoot
$env:NPM_CONFIG_CACHE = $npmCache
$env:MAVEN_OPTS = "-Dmaven.repo.local=$($mavenRepository.Replace('\', '/')) $env:MAVEN_OPTS".Trim()
$env:DB_HOST = "localhost"
$env:RABBITMQ_HOST = "localhost"
$env:HIGH_AVAILABILITY_GATEWAY_URL = "http://localhost:8081"
$env:SERVER_PORT = $BackendPort.ToString()
$env:NEXAMIND_CONTAINER_DOCKER_HOST = (docker context inspect --format '{{.Endpoints.docker.Host}}').Trim()

if (-not $env:JWT_SECRET) {
    $randomBytes = [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(48)
    $env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Engine is unavailable. Start Docker Desktop first."
}

Push-Location $PSScriptRoot
try {
    docker compose --profile local --profile dev up -d postgres rabbitmq api-gateway adminer
} finally {
    Pop-Location
}

$backendLog = Join-Path $logRoot "backend.out.log"
$backendErrorLog = Join-Path $logRoot "backend.err.log"
$frontendLog = Join-Path $logRoot "frontend.out.log"
$frontendErrorLog = Join-Path $logRoot "frontend.err.log"

if (-not (Get-NetTCPConnection -LocalPort $BackendPort -State Listen -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath (Join-Path $projectRoot "NexaMind\mvnw.cmd") `
        -ArgumentList @("spring-boot:run", "-Dspring-boot.run.profiles=dev") `
        -WorkingDirectory (Join-Path $projectRoot "NexaMind") `
        -WindowStyle Hidden `
        -RedirectStandardOutput $backendLog `
        -RedirectStandardError $backendErrorLog | Out-Null
}

if (-not (Get-NetTCPConnection -LocalPort $FrontendPort -State Listen -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath "npm.cmd" `
        -ArgumentList @("run", "dev", "--", "-p", $FrontendPort.ToString()) `
        -WorkingDirectory (Join-Path $projectRoot "nexamind-frontend") `
        -WindowStyle Hidden `
        -RedirectStandardOutput $frontendLog `
        -RedirectStandardError $frontendErrorLog | Out-Null
}

Write-Output "NexaMind local environment is starting:"
Write-Output "Frontend: http://localhost:$FrontendPort"
Write-Output "Backend:  http://localhost:$BackendPort/api"
Write-Output "Local data: $localRoot"

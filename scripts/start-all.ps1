param(
    [switch]$Pull,
    [switch]$Recreate
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot ".env"
$envExample = Join-Path $repoRoot ".env.example"

Write-Host "Repo root: $repoRoot"

if (-not (Test-Path $envFile)) {
    if (Test-Path $envExample) {
        Write-Host "Missing .env, copying from .env.example..."
        Copy-Item $envExample $envFile
    } else {
        throw "Missing .env and .env.example; please prepare environment variables."
    }
}

Push-Location $repoRoot
try {
    if ($Pull) {
        Write-Host "Pulling images..."
        docker-compose pull
    }

    if ($Recreate) {
        Write-Host "Stopping and removing existing containers..."
        docker-compose down
    }

    Write-Host "Starting services (detached)..."
    docker-compose up -d

    Write-Host "Current service status:"
    docker-compose ps
}
finally {
    Pop-Location
}


# Gå till projektets rotmapp
Set-Location $PSScriptRoot

# Kontrollera att .env finns
if (-not (Test-Path ".env")) {
    throw "Filen .env saknas!"
}

# Läs in lösenorden från .env
Get-Content ".env" | ForEach-Object {
    if ($_ -match '^\s*(WAC_(?:ADMIN|WORKSHOP|VIEWER)_PASSWORD|DB_USERNAME|DB_PASSWORD)\s*=(.*)$') {
        [Environment]::SetEnvironmentVariable(
            $matches[1],
            $matches[2].Trim(),
            "Process"
        )
    }
}

# Kontrollera att alla variabler finns
$required = @(
    "WAC_ADMIN_PASSWORD",
    "WAC_WORKSHOP_PASSWORD",
    "WAC_VIEWER_PASSWORD"
)

foreach ($name in $required) {
    if ([string]::IsNullOrEmpty(
        [Environment]::GetEnvironmentVariable($name, "Process")
    )) {
        throw "Miljövariabel saknas: $name"
    }
}

# Starta Wigell AutoCore
Write-Host "Startar Wigell AutoCore..."
mvn spring-boot:run

#!/usr/bin/env bash
set -euo pipefail

# Gå till projektets rotmapp
cd "$(dirname "$0")"

# Kontrollera att .env finns
if [[ ! -f ".env" ]]; then
    echo "Filen .env saknas!"
    exit 1
fi

# Läs in miljövariabler från .env
while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"

    if [[ "$line" =~ ^[[:space:]]*(WAC_(ADMIN|WORKSHOP|VIEWER)_PASSWORD|DB_USERNAME|DB_PASSWORD)=(.*)$ ]]; then
        export "${BASH_REMATCH[1]}=${BASH_REMATCH[3]}"
    fi
done < ".env"

# Kontrollera obligatoriska variabler
required=(
    WAC_ADMIN_PASSWORD
    WAC_WORKSHOP_PASSWORD
    WAC_VIEWER_PASSWORD
    DB_PASSWORD
)

for name in "${required[@]}"; do
    if [[ -z "${!name:-}" ]]; then
        echo "Miljövariabel saknas: $name"
        exit 1
    fi
done

echo "Startar Wigell AutoCore..."
mvn spring-boot:run
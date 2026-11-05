param([int]$Puerto = 8765)
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try { python -m backend.servidor --port $Puerto }
finally { Pop-Location }

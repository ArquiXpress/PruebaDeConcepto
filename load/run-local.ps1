param(
  [string]$BaseUrl = "http://localhost:8080",
  [int]$Vus = 50,
  [string]$Duration = "1m"
)

$env:BASE_URL = $BaseUrl
$env:VUS = "$Vus"
$env:DURATION = $Duration

k6 run .\load\search_peak.js
k6 run .\load\checkout_concurrency.js
k6 run .\load\rbac_smoke.js

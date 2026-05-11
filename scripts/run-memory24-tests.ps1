param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$CasesPath = '.\test-data\memory_test_cases_24turn_3groups.json',
    [string]$OutputDir = '.\test-output',
    [int]$DelayMs = 200
)

$ErrorActionPreference = 'Stop'

if (!(Test-Path $CasesPath)) { throw "测试用例文件不存在: $CasesPath" }
if (!(Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir | Out-Null }

function Normalize-Text {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return '' }
    $normalized = $Text -replace "`r", ' ' -replace "`n", ' '
    $normalized = $normalized -replace '\s{2,}', ' '
    return $normalized.Trim()
}

$timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$transcriptCsv = Join-Path $OutputDir "memory24_transcripts_$timestamp.csv"
$checkpointsCsv = Join-Path $OutputDir "memory24_checkpoints_$timestamp.csv"
$summaryJson = Join-Path $OutputDir "memory24_summary_$timestamp.json"

$cases = Get-Content $CasesPath -Raw -Encoding UTF8 | ConvertFrom-Json
$transcriptRows = @()
$checkpointRows = @()
$summary = @()

foreach ($case in $cases) {
    Write-Host ("开始执行 {0} - {1}" -f $case.caseId, $case.theme)
    $responsesByTurn = @{}
    $turnCount = $case.messages.Count

    for ($i = 0; $i -lt $turnCount; $i++) {
        $turn = $i + 1
        $message = [string]$case.messages[$i]
        $body = @{ memoryId = [int64]$case.memoryId; message = $message } | ConvertTo-Json -Depth 5
        try {
            $response = Invoke-WebRequest -Uri "$BaseUrl/xiaozhi/chat" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body -TimeoutSec 180
            $answer = Normalize-Text $response.Content
        }
        catch {
            $answer = "[REQUEST_FAILED] $($_.Exception.Message)"
        }

        $responsesByTurn[$turn] = $answer
        $isCheckpoint = @($case.checkpoints | Where-Object { $_.turn -eq $turn }).Count -gt 0
        $transcriptRows += [PSCustomObject]@{
            用例ID = $case.caseId
            主题 = $case.theme
            memoryId = $case.memoryId
            轮次 = $turn
            用户输入 = $message
            Agent回复 = $answer
            是否回忆轮次 = if ($isCheckpoint) { '是' } else { '否' }
        }

        Start-Sleep -Milliseconds $DelayMs
    }

    foreach ($checkpoint in $case.checkpoints) {
        $turn = [int]$checkpoint.turn
        $answer = [string]$responsesByTurn[$turn]
        foreach ($expected in $checkpoint.expected) {
            $checkpointRows += [PSCustomObject]@{
                用例ID = $case.caseId
                主题 = $case.theme
                memoryId = $case.memoryId
                回忆轮次 = $turn
                回忆问题 = $checkpoint.question
                信息点ID = $expected.id
                信息点类别 = $expected.category
                期望信息 = $expected.value
                Agent回复 = $answer
                记忆类型 = if ($turn -ge 19) { '中期记忆' } else { '短期记忆' }
                人工复核结果 = ''
                人工复核得分 = ''
                备注 = ''
            }
        }
    }

    $summary += [PSCustomObject]@{
        caseId = $case.caseId
        theme = $case.theme
        memoryId = $case.memoryId
        totalTurns = $turnCount
        checkpointTurns = @($case.checkpoints | ForEach-Object { $_.turn })
    }
}

$transcriptRows | Export-Csv -Path $transcriptCsv -NoTypeInformation -Encoding UTF8
$checkpointRows | Export-Csv -Path $checkpointsCsv -NoTypeInformation -Encoding UTF8
$summary | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryJson -Encoding UTF8

Write-Host "执行完成"
Write-Host "TRANSCRIPTS=$transcriptCsv"
Write-Host "CHECKPOINTS=$checkpointsCsv"
Write-Host "SUMMARY=$summaryJson"

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$InputCsv = ".\test-data\qa_test_set_100.csv",
    [string]$OutputDir = ".\test-output",
    [int]$StartMemoryId = 900001,
    [int]$DelayMs = 300
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $InputCsv)) {
    throw "输入文件不存在: $InputCsv"
}

if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outputCsv = Join-Path $OutputDir "qa_result_$timestamp.csv"

$rows = Import-Csv -Path $InputCsv -Encoding UTF8
$resultRows = @()

function Normalize-Text {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return ""
    }

    $normalized = $Text -replace "`r", " " -replace "`n", " "
    $normalized = $normalized -replace "\s{2,}", " "
    return $normalized.Trim()
}

function Get-KeywordScore {
    param(
        [string]$Answer,
        [string]$KeywordText
    )

    $answerText = Normalize-Text $Answer
    if ([string]::IsNullOrWhiteSpace($answerText)) {
        return [PSCustomObject]@{
            HitCount = 0
            TotalCount = 0
            Score = 0
            AutoJudgement = "错误"
            HitKeywords = ""
        }
    }

    $keywords = $KeywordText -split "；|\|" | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    if ($keywords.Count -eq 0) {
        return [PSCustomObject]@{
            HitCount = 0
            TotalCount = 0
            Score = 0
            AutoJudgement = "待人工判断"
            HitKeywords = ""
        }
    }

    $hitKeywords = @()
    foreach ($kw in $keywords) {
        if ($answerText -like "*$kw*") {
            $hitKeywords += $kw
        }
    }

    $hitCount = $hitKeywords.Count
    $totalCount = $keywords.Count
    $ratio = 0
    if ($totalCount -gt 0) {
        $ratio = $hitCount / $totalCount
    }

    $score = 0
    $judgement = "错误"
    if ($ratio -ge 0.8) {
        $score = 1
        $judgement = "正确"
    } elseif ($ratio -ge 0.4) {
        $score = 0.5
        $judgement = "部分正确"
    }

    return [PSCustomObject]@{
        HitCount = $hitCount
        TotalCount = $totalCount
        Score = $score
        AutoJudgement = $judgement
        HitKeywords = ($hitKeywords -join "；")
    }
}

for ($i = 0; $i -lt $rows.Count; $i++) {
    $row = $rows[$i]
    $memoryId = $StartMemoryId + $i
    $body = @{
        memoryId = $memoryId
        message = $row.问题
    } | ConvertTo-Json -Depth 5

    Write-Host ("[{0}/{1}] 正在测试 {2}" -f ($i + 1), $rows.Count, $row.用例ID)

    try {
        $response = Invoke-WebRequest `
            -Uri "$BaseUrl/xiaozhi/chat" `
            -Method Post `
            -ContentType "application/json; charset=utf-8" `
            -Body $body `
            -TimeoutSec 180

        $answer = Normalize-Text $response.Content
        $scoreResult = Get-KeywordScore -Answer $answer -KeywordText $row.'标准答案/关键点'

        $resultRows += [PSCustomObject]@{
            用例ID = $row.用例ID
            问题 = $row.问题
            问题类型 = $row.问题类型
            标准答案_关键点 = $row.'标准答案/关键点'
            关联知识文件 = $row.关联知识文件
            memoryId = $memoryId
            系统回答 = $answer
            是否命中知识库 = ""
            自动判定 = $scoreResult.AutoJudgement
            自动得分 = $scoreResult.Score
            命中关键点数 = $scoreResult.HitCount
            关键点总数 = $scoreResult.TotalCount
            命中关键词 = $scoreResult.HitKeywords
            人工复核结果 = ""
            人工复核得分 = ""
            人工判定说明 = ""
            备注 = ""
        }
    }
    catch {
        $resultRows += [PSCustomObject]@{
            用例ID = $row.用例ID
            问题 = $row.问题
            问题类型 = $row.问题类型
            标准答案_关键点 = $row.'标准答案/关键点'
            关联知识文件 = $row.关联知识文件
            memoryId = $memoryId
            系统回答 = ""
            是否命中知识库 = ""
            自动判定 = "请求失败"
            自动得分 = 0
            命中关键点数 = 0
            关键点总数 = 0
            命中关键词 = ""
            人工复核结果 = ""
            人工复核得分 = ""
            人工判定说明 = $_.Exception.Message
            备注 = "接口调用失败"
        }
    }

    Start-Sleep -Milliseconds $DelayMs
}

$resultRows | Export-Csv -Path $outputCsv -NoTypeInformation -Encoding UTF8

Write-Host ""
Write-Host "测试完成，结果文件: $outputCsv"

$validRows = $resultRows | Where-Object { $_.自动判定 -ne "请求失败" }
if ($validRows.Count -gt 0) {
    $autoScoreSum = ($validRows | Measure-Object -Property 自动得分 -Sum).Sum
    $autoAccuracy = [math]::Round(($autoScoreSum / $rows.Count) * 100, 2)
    Write-Host "自动加权准确率(仅供初筛): $autoAccuracy%"
}

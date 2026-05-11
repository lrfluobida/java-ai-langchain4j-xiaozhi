param(
    [string]$TranscriptCsv,
    [string]$CheckpointCsv
)

function Get-ExcelColName {
    param([int]$ColumnNumber)
    $name = ''
    while ($ColumnNumber -gt 0) {
        $ColumnNumber--
        $name = [char](65 + ($ColumnNumber % 26)) + $name
        $ColumnNumber = [math]::Floor($ColumnNumber / 26)
    }
    return $name
}

if (!(Test-Path $TranscriptCsv)) { throw "找不到对话结果文件: $TranscriptCsv" }
if (!(Test-Path $CheckpointCsv)) { throw "找不到评分结果文件: $CheckpointCsv" }

$transcriptPath = (Resolve-Path $TranscriptCsv).Path
$checkpointPath = (Resolve-Path $CheckpointCsv).Path
$xlsxPath = Join-Path (Split-Path $checkpointPath -Parent) (([System.IO.Path]::GetFileNameWithoutExtension($checkpointPath)) + '.review.xlsx')

$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
$wb = $null

function Import-CsvToSheet {
    param($Workbook, $SheetName, $CsvPath)
    $ws = $Workbook.Worksheets.Add()
    $ws.Name = $SheetName
    $qt = $ws.QueryTables.Add("TEXT;$CsvPath", $ws.Range('A1'))
    $qt.TextFilePlatform = 65001
    $qt.TextFileParseType = 1
    $qt.TextFileCommaDelimiter = $true
    $qt.TextFileTextQualifier = 1
    $qt.AdjustColumnWidth = $true
    [void]$qt.Refresh($false)
    $qt.Delete()
    return $ws
}

try {
    $wb = $excel.Workbooks.Add()
    while ($wb.Worksheets.Count -gt 1) { $wb.Worksheets.Item($wb.Worksheets.Count).Delete() }
    $wb.Worksheets.Item(1).Name = 'Summary'
    $summary = $wb.Worksheets.Item('Summary')

    $transcriptWs = Import-CsvToSheet -Workbook $wb -SheetName 'Transcripts' -CsvPath $transcriptPath
    $checkpointWs = Import-CsvToSheet -Workbook $wb -SheetName 'Checkpoints' -CsvPath $checkpointPath

    foreach ($ws in @($transcriptWs, $checkpointWs)) {
        $usedRange = $ws.UsedRange
        $rowCount = $usedRange.Rows.Count
        $colCount = $usedRange.Columns.Count
        $lastColLetter = Get-ExcelColName $colCount
        $tableRange = $ws.Range("A1:${lastColLetter}$rowCount")
        $listObject = $ws.ListObjects.Add(1, $tableRange, $null, 1)
        $listObject.TableStyle = 'TableStyleMedium2'
        $ws.Rows.Item(1).Font.Bold = $true
        $ws.Rows.Item(1).Interior.Color = 0xD9EAF7
        $excel.ActiveWindow.SplitRow = 1
        $excel.ActiveWindow.FreezePanes = $true
    }

    $transcriptWs.Columns.Item(1).ColumnWidth = 12
    $transcriptWs.Columns.Item(2).ColumnWidth = 24
    $transcriptWs.Columns.Item(3).ColumnWidth = 12
    $transcriptWs.Columns.Item(4).ColumnWidth = 8
    $transcriptWs.Columns.Item(5).ColumnWidth = 42
    $transcriptWs.Columns.Item(6).ColumnWidth = 75
    $transcriptWs.Columns.Item(7).ColumnWidth = 12
    $transcriptWs.Columns('E:F').WrapText = $true

    $checkpointWs.Columns.Item(1).ColumnWidth = 12
    $checkpointWs.Columns.Item(2).ColumnWidth = 24
    $checkpointWs.Columns.Item(3).ColumnWidth = 12
    $checkpointWs.Columns.Item(4).ColumnWidth = 10
    $checkpointWs.Columns.Item(5).ColumnWidth = 34
    $checkpointWs.Columns.Item(6).ColumnWidth = 10
    $checkpointWs.Columns.Item(7).ColumnWidth = 14
    $checkpointWs.Columns.Item(8).ColumnWidth = 24
    $checkpointWs.Columns.Item(9).ColumnWidth = 78
    $checkpointWs.Columns.Item(10).ColumnWidth = 12
    $checkpointWs.Columns.Item(11).ColumnWidth = 14
    $checkpointWs.Columns.Item(12).ColumnWidth = 12
    $checkpointWs.Columns.Item(13).ColumnWidth = 18
    $checkpointWs.Columns('E:I').WrapText = $true

    $manualResultCol = 11
    $manualScoreCol = 12
    $rowCount = $checkpointWs.UsedRange.Rows.Count
    $manualResultColLetter = Get-ExcelColName $manualResultCol
    $manualResultRange = $checkpointWs.Range("${manualResultColLetter}2:${manualResultColLetter}${rowCount}")
    $manualScoreRange = $checkpointWs.Range("L2:L${rowCount}")

    $manualResultRange.Validation.Delete()
    $manualResultRange.Validation.Add(3, 1, 1, '正确,部分正确,错误')
    $manualResultRange.Validation.IgnoreBlank = $true
    $manualResultRange.Validation.InCellDropdown = $true

    for ($r = 2; $r -le $rowCount; $r++) {
        $manualScoreRange.Cells.Item($r - 1, 1).Formula = ('=IF({0}{1}="正确",1,IF({0}{1}="部分正确",0.5,IF({0}{1}="错误",0,"")))' -f $manualResultColLetter, $r)
    }

    $manualResultRange.FormatConditions.Delete()
    $fc1 = $manualResultRange.FormatConditions.Add(1, 3, '="正确"')
    $fc1.Interior.Color = 0xC6EFCE
    $fc2 = $manualResultRange.FormatConditions.Add(1, 3, '="部分正确"')
    $fc2.Interior.Color = 0xFFEB9C
    $fc3 = $manualResultRange.FormatConditions.Add(1, 3, '="错误"')
    $fc3.Interior.Color = 0xFFC7CE

    $summary.Range('A1').Value2 = '指标'
    $summary.Range('B1').Value2 = '值'
    $summary.Range('A2').Value2 = '测试组数'
    $summary.Range('B2').Formula = '=COUNTA(UNIQUE(Checkpoints!A2:A999))'
    $summary.Range('A3').Value2 = '总对话轮次'
    $summary.Range('B3').Formula = '=COUNTA(Transcripts!D2:D999)'
    $summary.Range('A4').Value2 = '总信息点数'
    $summary.Range('B4').Formula = '=COUNTA(Checkpoints!F2:F999)'
    $summary.Range('A6').Value2 = '人工正确数'
    $summary.Range('B6').Formula = '=COUNTIF(Checkpoints!K:K,"正确")'
    $summary.Range('A7').Value2 = '人工部分正确数'
    $summary.Range('B7').Formula = '=COUNTIF(Checkpoints!K:K,"部分正确")'
    $summary.Range('A8').Value2 = '人工错误数'
    $summary.Range('B8').Formula = '=COUNTIF(Checkpoints!K:K,"错误")'
    $summary.Range('A9').Value2 = '总体加权保持率'
    $summary.Range('B9').Formula = '=SUM(Checkpoints!L:L)/B4'
    $summary.Range('A10').Value2 = '中期记忆保持率'
    $summary.Range('B10').Formula = '=SUMIFS(Checkpoints!L:L,Checkpoints!J:J,"中期记忆")/COUNTIF(Checkpoints!J:J,"中期记忆")'
    $summary.Range('A11').Value2 = '短期记忆保持率'
    $summary.Range('B11').Formula = '=SUMIFS(Checkpoints!L:L,Checkpoints!J:J,"短期记忆")/COUNTIF(Checkpoints!J:J,"短期记忆")'
    $summary.Range('A13').Value2 = '复核建议'
    $summary.Range('B13').Value2 = '先筛选 Checkpoints 表中的 人工复核结果 为空 的行，逐条判断 Agent 回复是否覆盖期望信息。'
    $summary.Range('B13').WrapText = $true
    $summary.Range('A1:B13').Columns.AutoFit() | Out-Null
    $summary.Range('B9:B11').NumberFormat = '0.00%'
    $summary.Rows.Item(1).Font.Bold = $true
    $summary.Range('A1:B13').Borders.LineStyle = 1

    $wb.Worksheets.Item('Summary').Move($wb.Worksheets.Item(1))
    $wb.SaveAs($xlsxPath, 51)
    $wb.Close($true)
    Write-Output $xlsxPath
}
finally {
    if ($wb) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($wb) | Out-Null }
    if ($excel) {
        $excel.Quit()
        [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}



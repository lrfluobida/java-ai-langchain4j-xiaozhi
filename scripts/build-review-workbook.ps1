param(
    [string]$CsvPath = '.\test-output\qa_result_20260311_160710.csv'
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

$csvPathResolved = (Resolve-Path $CsvPath).Path
$xlsxPath = [System.IO.Path]::ChangeExtension($csvPathResolved, '.review.xlsx')

$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
$wb = $null
$ws = $null
$summary = $null
$listObject = $null
$queryTable = $null

try {
    $wb = $excel.Workbooks.Add()
    $ws = $wb.Worksheets.Item(1)
    $ws.Name = 'QA Review'

    $queryTable = $ws.QueryTables.Add("TEXT;$csvPathResolved", $ws.Range('A1'))
    $queryTable.TextFilePlatform = 65001
    $queryTable.TextFileParseType = 1
    $queryTable.TextFileCommaDelimiter = $true
    $queryTable.TextFileTextQualifier = 1
    $queryTable.AdjustColumnWidth = $true
    $queryTable.Refresh($false)
    $queryTable.Delete()

    $usedRange = $ws.UsedRange
    $rowCount = $usedRange.Rows.Count
    $colCount = $usedRange.Columns.Count

    $headers = @{}
    for ($c = 1; $c -le $colCount; $c++) {
        $headers[$ws.Cells.Item(1, $c).Text] = $c
    }

    $ws.Rows.Item(1).Font.Bold = $true
    $ws.Rows.Item(1).Interior.Color = 0xD9EAF7
    $ws.Rows.Item(1).WrapText = $true
    $excel.ActiveWindow.SplitRow = 1
    $excel.ActiveWindow.FreezePanes = $true

    $lastColLetter = Get-ExcelColName $colCount
    $tableRange = $ws.Range("A1:${lastColLetter}$rowCount")
    $listObject = $ws.ListObjects.Add(1, $tableRange, $null, 1)
    $listObject.Name = 'QaReviewTable'
    $listObject.TableStyle = 'TableStyleMedium2'

    $ws.Columns.Item($headers['用例ID']).ColumnWidth = 10
    $ws.Columns.Item($headers['问题']).ColumnWidth = 28
    $ws.Columns.Item($headers['问题类型']).ColumnWidth = 12
    $ws.Columns.Item($headers['标准答案_关键点']).ColumnWidth = 24
    $ws.Columns.Item($headers['关联知识文件']).ColumnWidth = 16
    $ws.Columns.Item($headers['memoryId']).ColumnWidth = 12
    $ws.Columns.Item($headers['系统回答']).ColumnWidth = 75
    $ws.Columns.Item($headers['是否命中知识库']).ColumnWidth = 14
    $ws.Columns.Item($headers['自动判定']).ColumnWidth = 12
    $ws.Columns.Item($headers['自动得分']).ColumnWidth = 10
    $ws.Columns.Item($headers['命中关键词']).ColumnWidth = 18
    $ws.Columns.Item($headers['人工复核结果']).ColumnWidth = 14
    $ws.Columns.Item($headers['人工复核得分']).ColumnWidth = 12
    $ws.Columns.Item($headers['人工判定说明']).ColumnWidth = 28
    $ws.Columns.Item($headers['备注']).ColumnWidth = 18

    foreach ($name in @('问题','标准答案_关键点','系统回答','人工判定说明','备注')) {
        $ws.Columns.Item($headers[$name]).WrapText = $true
        $ws.Columns.Item($headers[$name]).VerticalAlignment = -4160
    }

    $manualResultCol = $headers['人工复核结果']
    $manualScoreCol = $headers['人工复核得分']
    $knowledgeHitCol = $headers['是否命中知识库']
    $autoJudgeCol = $headers['自动判定']

    $manualResultColLetter = Get-ExcelColName $manualResultCol
    $manualScoreColLetter = Get-ExcelColName $manualScoreCol

    $manualResultRange = $ws.Range("${manualResultColLetter}2:${manualResultColLetter}${rowCount}")
    $manualScoreRange = $ws.Range("${manualScoreColLetter}2:${manualScoreColLetter}${rowCount}")
    $knowledgeHitRange = $ws.Range("$(Get-ExcelColName $knowledgeHitCol)2:$(Get-ExcelColName $knowledgeHitCol)$rowCount")
    $autoJudgeRange = $ws.Range("$(Get-ExcelColName $autoJudgeCol)2:$(Get-ExcelColName $autoJudgeCol)$rowCount")

    $manualResultRange.Validation.Delete()
    $manualResultRange.Validation.Add(3, 1, 1, '正确,部分正确,错误')
    $manualResultRange.Validation.IgnoreBlank = $true
    $manualResultRange.Validation.InCellDropdown = $true

    $knowledgeHitRange.Validation.Delete()
    $knowledgeHitRange.Validation.Add(3, 1, 1, '是,否,待确认')
    $knowledgeHitRange.Validation.IgnoreBlank = $true
    $knowledgeHitRange.Validation.InCellDropdown = $true

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

    $autoJudgeRange.FormatConditions.Delete()
    $af1 = $autoJudgeRange.FormatConditions.Add(1, 3, '="正确"')
    $af1.Interior.Color = 0xE2F0D9
    $af2 = $autoJudgeRange.FormatConditions.Add(1, 3, '="部分正确"')
    $af2.Interior.Color = 0xFFF2CC
    $af3 = $autoJudgeRange.FormatConditions.Add(1, 3, '="错误"')
    $af3.Interior.Color = 0xFCE4D6
    $af4 = $autoJudgeRange.FormatConditions.Add(1, 3, '="请求失败"')
    $af4.Interior.Color = 0xD9E1F2

    $summary = $wb.Worksheets.Add()
    $summary.Name = 'Summary'
    $summary.Range('A1').Value2 = '指标'
    $summary.Range('B1').Value2 = '值'
    $summary.Range('A2').Value2 = '总题数'
    $summary.Range('B2').Formula = "=COUNTA('QA Review'!A:A)-1"
    $summary.Range('A3').Value2 = '自动正确数'
    $summary.Range('B3').Formula = '=COUNTIF(''QA Review''!I:I,"正确")'
    $summary.Range('A4').Value2 = '自动部分正确数'
    $summary.Range('B4').Formula = '=COUNTIF(''QA Review''!I:I,"部分正确")'
    $summary.Range('A5').Value2 = '自动错误数'
    $summary.Range('B5').Formula = '=COUNTIF(''QA Review''!I:I,"错误")'
    $summary.Range('A6').Value2 = '自动请求失败数'
    $summary.Range('B6').Formula = '=COUNTIF(''QA Review''!I:I,"请求失败")'
    $summary.Range('A7').Value2 = '自动加权准确率'
    $summary.Range('B7').Formula = '=SUM(''QA Review''!J:J)/B2'
    $summary.Range('A9').Value2 = '人工正确数'
    $summary.Range('B9').Formula = '=COUNTIF(''QA Review''!N:N,"正确")'
    $summary.Range('A10').Value2 = '人工部分正确数'
    $summary.Range('B10').Formula = '=COUNTIF(''QA Review''!N:N,"部分正确")'
    $summary.Range('A11').Value2 = '人工错误数'
    $summary.Range('B11').Formula = '=COUNTIF(''QA Review''!N:N,"错误")'
    $summary.Range('A12').Value2 = '人工严格准确率'
    $summary.Range('B12').Formula = '=B9/B2'
    $summary.Range('A13').Value2 = '人工加权准确率'
    $summary.Range('B13').Formula = '=SUM(''QA Review''!O:O)/B2'
    $summary.Range('A14').Value2 = '知识库命中率'
    $summary.Range('B14').Formula = '=COUNTIF(''QA Review''!H:H,"是")/B2'
    $summary.Range('A1:B14').Columns.AutoFit() | Out-Null
    $summary.Range('B7:B14').NumberFormat = '0.00%'
    $summary.Rows.Item(1).Font.Bold = $true
    $summary.Range('A1:B14').Borders.LineStyle = 1
    $summary.Range('A16').Value2 = '人工复核建议：先筛选自动判定=错误/请求失败，再逐条补充人工复核结果、人工判定说明和知识库命中情况。'
    $summary.Range('A16').WrapText = $true
    $summary.Range('A16:B16').Merge()
    $summary.Rows.Item(16).RowHeight = 36

    $ws.Activate()
    $wb.SaveAs($xlsxPath, 51)
    $wb.Close($true)
    Write-Output $xlsxPath
}
finally {
    if ($queryTable) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($queryTable) | Out-Null }
    if ($listObject) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($listObject) | Out-Null }
    if ($summary) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($summary) | Out-Null }
    if ($ws) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($ws) | Out-Null }
    if ($wb) { [System.Runtime.Interopservices.Marshal]::ReleaseComObject($wb) | Out-Null }
    if ($excel) {
        $excel.Quit()
        [System.Runtime.Interopservices.Marshal]::ReleaseComObject($excel) | Out-Null
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}


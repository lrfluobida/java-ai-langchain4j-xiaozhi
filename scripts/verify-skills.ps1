# Skill Verification Test Script (Windows PowerShell)
# Used to verify if skills are working correctly

$BASE_URL = "http://localhost:8080"
$MEMORY_ID = 577271

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Skill Verification Test" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

function Test-Step {
    param($message)
    Write-Host "[TEST] $message" -ForegroundColor Yellow
}

function Success {
    param($message)
    Write-Host "SUCCESS: $message" -ForegroundColor Green
}

function Error {
    param($message)
    Write-Host "ERROR: $message" -ForegroundColor Red
}

# 1. Clear previous session state
Test-Step "1. Clear session state"
try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/debug/skills/clear/$MEMORY_ID" -Method Delete
    $response | ConvertTo-Json
} catch {
    Error "Failed to clear session: $_"
}
Write-Host ""

# 2. First conversation - trigger skill
Test-Step "2. First conversation - trigger skill (should activate BASIC level)"
try {
    $body = @{
        memoryId = $MEMORY_ID
        message = "I want to register"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$BASE_URL/debug/skills/preview" -Method Post -Body $body -ContentType "application/json"
    $response | ConvertTo-Json
} catch {
    Error "Failed to preview skills: $_"
}
Write-Host ""

# 3. Check skill status
Test-Step "3. Check skill status (should see appointment activated)"
try {
    $status = Invoke-RestMethod -Uri "$BASE_URL/debug/skills/status/$MEMORY_ID"
    $status | ConvertTo-Json

    if ($status.activeSkillCount -gt 0) {
        Success "Skill activated, count: $($status.activeSkillCount)"
    } else {
        Error "Skill not activated"
    }
} catch {
    Error "Failed to get status: $_"
}
Write-Host ""

# 4. Compare with and without skill
Test-Step "4. Compare with and without skill"
try {
    $body = @{
        memoryId = $MEMORY_ID
        message = "I want to register"
    } | ConvertTo-Json

    $compare = Invoke-RestMethod -Uri "$BASE_URL/debug/skills/compare" -Method Post -Body $body -ContentType "application/json"
    $compare | ConvertTo-Json

    if ($compare.skillIsActive) {
        Success "Skill is active"
        Write-Host "  Injected skill content length: $($compare.difference) characters"
    } else {
        Error "Skill is not active"
    }
} catch {
    Error "Failed to compare: $_"
}
Write-Host ""

# 5. Simulate multiple rounds - test disclosure level upgrade
Test-Step "5. Simulate 3 rounds of conversation (should upgrade to STANDARD)"

try {
    # Round 2
    $body = @{ memoryId = $MEMORY_ID; message = "Cardiology" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BASE_URL/debug/skills/preview" -Method Post -Body $body -ContentType "application/json" | Out-Null

    # Round 3
    $body = @{ memoryId = $MEMORY_ID; message = "Tomorrow morning" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BASE_URL/debug/skills/preview" -Method Post -Body $body -ContentType "application/json" | Out-Null

    $status = Invoke-RestMethod -Uri "$BASE_URL/debug/skills/status/$MEMORY_ID"
    $status | ConvertTo-Json

    $skillDetails = $status.skillDetails.appointment
    if (-not $skillDetails) {
        $skillDetails = $status.skillDetails.'appointment-progressive'
    }

    if ($skillDetails.disclosureLevel -eq "STANDARD" -or $skillDetails.activeTurns -ge 3) {
        Success "Disclosure level upgraded: $($skillDetails.disclosureLevel), active turns: $($skillDetails.activeTurns)"
    } else {
        Write-Host "  Current disclosure level: $($skillDetails.disclosureLevel), active turns: $($skillDetails.activeTurns)"
    }
} catch {
    Error "Failed to simulate rounds: $_"
}
Write-Host ""

# 6. Simulate tool calls - test ADVANCED upgrade
Test-Step "6. Simulate 2 tool calls (should upgrade to ADVANCED)"

try {
    $body = @{ memoryId = $MEMORY_ID; skillName = "appointment" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BASE_URL/debug/skills/simulate-tool-call" -Method Post -Body $body -ContentType "application/json" | Out-Null
    Invoke-RestMethod -Uri "$BASE_URL/debug/skills/simulate-tool-call" -Method Post -Body $body -ContentType "application/json" | Out-Null

    $status = Invoke-RestMethod -Uri "$BASE_URL/debug/skills/status/$MEMORY_ID"
    $status | ConvertTo-Json

    $skillDetails = $status.skillDetails.appointment
    if (-not $skillDetails) {
        $skillDetails = $status.skillDetails.'appointment-progressive'
    }

    if ($skillDetails.disclosureLevel -eq "ADVANCED") {
        Success "Disclosure level upgraded to ADVANCED, tool calls: $($skillDetails.toolCallCount)"
    } else {
        Write-Host "  Current disclosure level: $($skillDetails.disclosureLevel), tool calls: $($skillDetails.toolCallCount)"
    }
} catch {
    Error "Failed to simulate tool calls: $_"
}
Write-Host ""

# 7. Test error marking
Test-Step "7. Test error marking (should immediately upgrade to ADVANCED)"

try {
    # Clear state first
    Invoke-RestMethod -Uri "$BASE_URL/debug/skills/clear/$MEMORY_ID" -Method Delete | Out-Null

    # Re-trigger
    $body = @{ memoryId = $MEMORY_ID; message = "I want to register" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BASE_URL/debug/skills/preview" -Method Post -Body $body -ContentType "application/json" | Out-Null

    # Mark error
    $body = @{ memoryId = $MEMORY_ID; skillName = "appointment" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BASE_URL/debug/skills/simulate-error" -Method Post -Body $body -ContentType "application/json" | Out-Null

    $status = Invoke-RestMethod -Uri "$BASE_URL/debug/skills/status/$MEMORY_ID"
    $status | ConvertTo-Json

    $skillDetails = $status.skillDetails.appointment
    if (-not $skillDetails) {
        $skillDetails = $status.skillDetails.'appointment-progressive'
    }

    if ($skillDetails.disclosureLevel -eq "ADVANCED" -and $skillDetails.hasError) {
        Success "Error marking effective, disclosure level upgraded to ADVANCED"
    } else {
        Write-Host "  Current disclosure level: $($skillDetails.disclosureLevel), has error: $($skillDetails.hasError)"
    }
} catch {
    Error "Failed to test error marking: $_"
}
Write-Host ""

# 8. Summary
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Verification checklist:"
Write-Host "  1. Can skill be triggered and activated"
Write-Host "  2. Is skill content injected into prompt"
Write-Host "  3. Does disclosure level upgrade with conversation rounds"
Write-Host "  4. Do tool calls trigger disclosure upgrade"
Write-Host "  5. Does error marking trigger disclosure upgrade"
Write-Host ""
Write-Host "If all tests pass, the Skill system is working correctly."
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Test in actual conversation"
Write-Host "  2. Observe if model follows skill instructions"
Write-Host "  3. Compare model behavior with/without skill enabled"

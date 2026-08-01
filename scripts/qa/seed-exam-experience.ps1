[CmdletBinding()]
param(
    [string]$BackendUrl = "http://localhost:18081",
    [string]$Database = "owlexa_qa",
    [string]$MySqlHost = "localhost",
    [string]$MySqlUser = "root",
    [string]$TeacherPhone = "0905555551",
    [string]$TeacherPassword = $env:QA_TEACHER_PASSWORD
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http
$expectedDatabase = "owlexa_qa"
$qaPrefix = "QA_EXAM_R5"
$assets = Join-Path $PSScriptRoot "assets"

if ($Database -ne $expectedDatabase) { throw "Refusing to run outside $expectedDatabase." }
if ([string]::IsNullOrWhiteSpace($TeacherPassword)) { $TeacherPassword = Read-Host "QA teacher password" -AsSecureString | ConvertFrom-SecureString }
# A SecureString cannot be sent as JSON. Require the caller to pass QA_TEACHER_PASSWORD instead.
if ($TeacherPassword -match "^[0-9A-F]{32,}$") { throw "Set QA_TEACHER_PASSWORD for non-interactive API login; do not persist it." }

function Invoke-MySqlScalar([string]$Sql) {
    $result = & mysql.exe -h $MySqlHost -u $MySqlUser -D $Database --batch --skip-column-names -e $Sql
    if ($LASTEXITCODE -ne 0) { throw "MySQL command failed." }
    return @($result)
}

function Require-One([string]$Name, [string]$Sql) {
    $rows = @(Invoke-MySqlScalar $Sql)
    if ($rows.Count -ne 1 -or [string]::IsNullOrWhiteSpace($rows[0])) { throw "$Name must resolve to exactly one record; found $($rows.Count)." }
    return $rows[0].Trim()
}

function Invoke-Api([string]$Method, [string]$Path, $Body = $null, [string]$Operation = $Path) {
    $headers = @{ Authorization = "Bearer $script:AccessToken" }
    $uri = "$($BackendUrl.TrimEnd('/'))$Path"
    try {
        if ($null -eq $Body) { return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers }
        $json = $Body | ConvertTo-Json -Depth 32 -Compress
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json" -Body $json
    } catch {
        $response = $_.Exception.Response
        $status = if ($response) { [int]$response.StatusCode } else { "NO_RESPONSE" }
        $responseBody = ""
        if ($response -and $response.GetResponseStream()) {
            $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            $reader.Dispose()
        }
        $responseBody = $responseBody -replace '(?i)(accessToken|refreshToken|authorization|password)\s*[:=]\s*[^,}\s]+', '$1=[REDACTED]'
        Write-Error "QA API failure | operation=$Operation | method=$Method | path=$Path | status=$status | body=$responseBody"
        throw
    }
}

function Upload-File([string]$Path) {
    $client = [System.Net.Http.HttpClient]::new()
    try {
        $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $script:AccessToken)
        $form = [System.Net.Http.MultipartFormDataContent]::new()
        $stream = [System.IO.File]::OpenRead($Path)
        $part = [System.Net.Http.StreamContent]::new($stream)
        $mimeType = if ($Path.EndsWith('.png')) { 'image/png' } else { 'audio/mpeg' }
        $part.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse($mimeType)
        $form.Add($part, "file", [System.IO.Path]::GetFileName($Path))
        $response = $client.PostAsync("$($BackendUrl.TrimEnd('/'))/api/files/upload", $form).GetAwaiter().GetResult()
        $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) { throw "Fixture upload failed: $($response.StatusCode)" }
        return $content | ConvertFrom-Json
    } finally {
        if ($stream) { $stream.Dispose() }
        if ($form) { $form.Dispose() }
        $client.Dispose()
    }
}

function Get-OrUploadFixture([string]$FileName) {
    $sql = "SELECT id, url FROM files WHERE center_id=$script:CenterId AND original_name='$FileName' AND deleted_at IS NULL ORDER BY id"
    $rows = @(Invoke-MySqlScalar $sql)
    if ($rows.Count -gt 1) { throw "Fixture $FileName is ambiguous; refusing to create a duplicate." }
    if ($rows.Count -eq 1) {
        $parts = $rows[0] -split "`t", 2
        return [pscustomobject]@{ id = [int64]$parts[0]; url = $parts[1] }
    }
    return Upload-File (Join-Path $assets $FileName)
}

function Get-Doc([string]$Text) {
    return @{ type = "doc"; content = @(@{ type = "paragraph"; content = @(@{ type = "text"; text = $Text }) }) }
}

function Get-QuestionPage([int]$CollectionId) { return (Invoke-Api GET "/teacher/questions?collectionId=$CollectionId&size=250").content }

function Get-OrCreateCollection {
    $matches = @((Invoke-Api GET "/teacher/question-collections") | Where-Object { $_.code -eq $qaPrefix })
    if ($matches.Count -gt 1) { throw "QA collection is ambiguous." }
    if ($matches.Count -eq 1) { return $matches[0] }
    return Invoke-Api POST "/teacher/question-collections" @{ code=$qaPrefix; name=$qaPrefix; description="QA-only Exam Experience R5 dataset" }
}

function Ensure-Questions([int64]$CollectionId, $Image) {
    $existing = @(Get-QuestionPage $CollectionId)
    if ($existing.Count -eq 0) {
        $questions = 1..200 | ForEach-Object {
            @{ type="MULTIPLE_CHOICE"; sectionCode="QA_PERF"; displayOrder=$_; content="QA performance question $_"; difficulty="EASY"; points=1; options=@(
                @{content="Answer A";isCorrect=$true}, @{content="Answer B";isCorrect=$false}, @{content="Answer C";isCorrect=$false}, @{content="Answer D";isCorrect=$false}) }
        }
        $payload = @{ version="2.0"; questions=$questions } | ConvertTo-Json -Depth 10 -Compress
        Invoke-Api POST "/teacher/questions/import" @{ collectionId=$CollectionId; json=$payload } | Out-Null
        $existing = @(Get-QuestionPage $CollectionId)
    }
    $performance = @($existing | Where-Object { $_.sectionCode -eq "QA_PERF" })
    if ($performance.Count -ne 200) { throw "Expected exactly 200 QA_PERF questions; found $($performance.Count)." }
    $byOrder = @{}; $performance | ForEach-Object { $byOrder[[int]$_.displayOrder] = $_ }
    if ((1..200 | Where-Object { -not $byOrder.ContainsKey($_) }).Count -ne 0) { throw "Performance question orders are incomplete." }
    $rich = @($existing | Where-Object { $_.sectionCode -eq "QA_RICH" })
    if ($rich.Count -eq 2) {
        $richQuestion = @($rich | Where-Object { $_.displayOrder -eq 201 -and $_.type -eq "MULTIPLE_CHOICE" })
        $essay = @($rich | Where-Object { $_.displayOrder -eq 202 -and $_.type -eq "ESSAY" })
        if ($richQuestion.Count -ne 1 -or $essay.Count -ne 1) { throw "Existing QA_RICH questions are inconsistent." }
        return [pscustomobject]@{ performance=$byOrder; rich=$richQuestion[0]; essay=$essay[0] }
    }
    if ($rich.Count -ne 0) { throw "Unexpected partial QA_RICH state." }
    $richContent = @{ type="doc"; content=@(
        @{type="heading";attrs=@{level=2};content=@(@{type="text";text="QA rich question"})},
        @{type="paragraph";content=@(@{type="text";text="Bold and italic formatting are exercised in the shared instructions."})},
        @{type="image";attrs=@{fileId=$Image.id;src=$Image.url;alt="QA diagram";title="QA diagram"}},
        @{type="table";content=@(@{type="tableRow";content=@(@{type="tableHeader";content=@(@{type="paragraph";content=@(@{type="text";text="Column"})})},@{type="tableHeader";content=@(@{type="paragraph";content=@(@{type="text";text="Value"})})})})}
    ) }
    $richQuestion = Invoke-Api POST "/teacher/questions" @{collectionId=$CollectionId;sectionCode="QA_RICH";displayOrder=201;type="MULTIPLE_CHOICE";content=$richContent;difficulty="MEDIUM";points=1;options=@(@{content="Correct";isCorrect=$true;displayOrder=1},@{content="Incorrect";isCorrect=$false;displayOrder=2})}
    $essay = Invoke-Api POST "/teacher/questions" @{collectionId=$CollectionId;sectionCode="QA_RICH";displayOrder=202;type="ESSAY";content=(Get-Doc "Write a detailed QA essay response. " + ("Long scrolling content. " * 80));difficulty="HARD";points=10;options=@()}
    return [pscustomobject]@{ performance=$byOrder; rich=$richQuestion; essay=$essay }
}

function Get-OrCreatePublishedAssessment([string]$Title, $Items, $Content, $AudioFileId) {
    $matches = @((Invoke-Api GET ("/teacher/assessments?search=" + [uri]::EscapeDataString($Title) + "&size=20")).content | Where-Object { $_.title -eq $Title })
    if ($matches.Count -gt 1) { throw "Assessment $Title is ambiguous." }
    if ($matches.Count -eq 1) {
        $detail = Invoke-Api GET "/teacher/assessments/$($matches[0].id)"
        if ($detail.status -ne "PUBLISHED" -or @($detail.items).Count -ne @($Items).Count) { throw "Existing assessment $Title is inconsistent." }
        return $detail
    }
    $request = @{title=$Title;type="QUIZ";content=$Content;playbackMode="PRACTICE";items=$Items}
    if ($null -ne $AudioFileId) { $request.audioFileId = [int64]$AudioFileId }
    $created = Invoke-Api POST "/teacher/assessments" $request
    return Invoke-Api POST "/teacher/assessments/$($created.id)/publish"
}

function Get-OrCreatePublishedAssignment([string]$Title, [int64]$AssessmentId) {
    $matches = @((Invoke-Api GET ("/teacher/assignments?search=" + [uri]::EscapeDataString($Title) + "&size=20")).content | Where-Object { $_.title -eq $Title })
    if ($matches.Count -gt 1) { throw "Assignment $Title is ambiguous." }
    if ($matches.Count -eq 1) {
        $detail = Invoke-Api GET "/teacher/assignments/$($matches[0].id)"
        if ($detail.status -ne "ACTIVE") { throw "Existing assignment $Title is not ACTIVE." }
        return $detail
    }
    $now=[DateTimeOffset]::UtcNow
    $created=Invoke-Api POST "/teacher/assignments" @{assessmentId=$AssessmentId;title=$Title;description="QA-only assignment";openAt=$now.AddMinutes(-5).ToString("o");dueAt=$now.AddDays(14).ToString("o");attemptLimit=1;targets=@(@{targetType="CLASS";classId=[int64]$script:ClassId})}
    return Invoke-Api POST "/teacher/assignments/$($created.id)/publish"
}

# Mandatory guards before all mutation.
if ((Invoke-MySqlScalar "SELECT DATABASE()") -ne $expectedDatabase) { throw "Database guard failed." }
$history = Invoke-MySqlScalar "SELECT CONCAT(SUM(success=1),'|',MAX(CAST(version AS UNSIGNED))) FROM flyway_schema_history"
if ($history -ne "12|12") { throw "Flyway guard failed: expected 12 successful migrations at version 12." }

$script:CenterId = Require-One "QA center" "SELECT id FROM centers WHERE name='Owlexa Central Branch - District 1'"
$script:ClassId = Require-One "QA class" "SELECT c.id FROM classes c JOIN centers ce ON ce.id=c.center_id WHERE ce.id=$script:CenterId AND c.name='TOEIC-750-K24' AND c.status='ACTIVE'"
$teacherId = Require-One "QA teacher" "SELECT u.id FROM users u JOIN membership m ON m.user_id=u.id WHERE u.email='teacher.nam@owlexa.edu.vn' AND u.role='TEACHER' AND m.center_id=$script:CenterId"
foreach($email in @('student.an@owlexa.edu.vn','student.binh@owlexa.edu.vn')) { Require-One "Active enrolled $email" "SELECT u.id FROM users u JOIN membership m ON m.user_id=u.id AND m.center_id=$script:CenterId JOIN class_enrollments e ON e.student_user_id=u.id AND e.class_id=$script:ClassId AND e.status='ACTIVE' WHERE u.email='$email' AND u.role='STUDENT'" | Out-Null }

$login = Invoke-RestMethod -Method POST -Uri "$($BackendUrl.TrimEnd('/'))/auth/login" -ContentType 'application/json' -Body (@{phoneNumber=$TeacherPhone;password=$TeacherPassword;deviceName='QA Seed Script';deviceType='DESKTOP'} | ConvertTo-Json -Compress)
$script:AccessToken = $login.accessToken
if ([string]::IsNullOrWhiteSpace($script:AccessToken)) { throw "Teacher login did not return an access token." }
Invoke-Api GET '/teacher/question-collections' | Out-Null # verifies tenant resolution through the actual authenticated request.

$audio = Get-OrUploadFixture 'qa-exam-r5-audio.mp3'
$image = Get-OrUploadFixture 'qa-exam-r5-image.png'
$collection = Get-OrCreateCollection
$questions = Ensure-Questions $collection.id $image
$shared = @{type='doc';content=@(@{type='heading';attrs=@{level=1};content=@(@{type='text';text='QA Exam Workspace'})},@{type='paragraph';content=@(@{type='text';text='Long shared instructions with rich formatting and a table.'})},@{type='image';attrs=@{fileId=$image.id;src=$image.url;alt='QA diagram';title='QA diagram'}})}
$richItems=@(@{questionId=$questions.rich.id;points=1;displayOrder=1},@{questionId=$questions.essay.id;points=10;displayOrder=2})
$rich=Get-OrCreatePublishedAssessment 'QA_EXAM_R5_RICH' $richItems $shared $audio.id
Get-OrCreatePublishedAssignment 'QA_EXAM_R5_RICH' $rich.id | Out-Null
foreach($count in @(50,100,200)) { $items=1..$count | ForEach-Object { @{questionId=$questions.performance[$_].id;points=1;displayOrder=$_} }; $assessment=Get-OrCreatePublishedAssessment ("QA_EXAM_R5_PERF_{0:D3}" -f $count) $items (Get-Doc "QA performance assessment $count") $null; Get-OrCreatePublishedAssignment ("QA_EXAM_R5_PERF_{0:D3}" -f $count) $assessment.id | Out-Null }
Write-Output "QA seed completed without logging credentials or tokens."

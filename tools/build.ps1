<#
.SYNOPSIS
    400AF 一键构建：自动判定升版本 -> 构建 -> 存档到 dist\<版本>\ -> 更新清单

.PARAMETER Kind
    auto（默认）| block（加方块）| update（普通更新）

.PARAMETER Note
    本次版本说明，写进 dist\版本历史.json

.PARAMETER NoBump
    不升版本，用当前版本号重新构建（用于修构建环境，不产生新版本）

.PARAMETER NoBuild
    跳过实际构建，只升版本 + 存档（用于先记录版本，稍后构建）

.EXAMPLE
    .\tools\build.ps1 -Note "加了车体侧板"

.EXAMPLE
    .\tools\build.ps1 -Kind update -Note "修了车门开合动画"
#>
param(
    [ValidateSet('auto', 'block', 'update')][string]$Kind = 'auto',
    [string]$Note = '',
    [switch]$NoBump,
    [switch]$NoBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'version.ps1')
. (Join-Path $PSScriptRoot 'detect-change.ps1')

$GradleProps = Join-Path $ProjectRoot 'gradle.properties'
$DistDir = Join-Path $ProjectRoot 'dist'
if (-not (Test-Path $DistDir)) { New-Item -ItemType Directory $DistDir -Force | Out-Null }

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host " 400AF 构建流水线" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

# ---------- 1. 决定版本号 ----------
$current = Get-CurrentModVersion $GradleProps
Write-Host "[1/5] 当前版本: $current"

if ($NoBump) {
    $newVersion = $current
    $changeKind = 'nobump'
    Write-Host "      -NoBump：保持版本号不变" -ForegroundColor Yellow
}
else {
    $decision = Resolve-ChangeKind -ProjectRoot $ProjectRoot -Kind $Kind
    Write-Host "      变动判定: $($decision.Kind)  —— $($decision.Reason)" -ForegroundColor Gray

    $bump = Get-NextAF400Version -Current $current -Kind $decision.Kind
    $newVersion = $bump.Version
    $changeKind = $decision.Kind
    Write-Host "      $($bump.Reason)" -ForegroundColor Green
}

# 写回 gradle.properties
Set-ModVersion -GradlePropertiesPath $GradleProps -Version $newVersion | Out-Null
Write-Host "      已写入 gradle.properties: mod_version=$newVersion"

# ---------- 2. 构建 ----------
$jarOut = Join-Path $DistDir $newVersion
New-Item -ItemType Directory $jarOut -Force | Out-Null

$built = $false
$builtJar = $null
if (-not $NoBuild) {
    Write-Host "[2/5] 开始构建……" -ForegroundColor Cyan

    # 构建方式：
    #   gradle —— 标准方式，需要能访问 maven.neoforged.net
    #   manual —— 手工方式，用本地 neoformruntime 缓存里的 Minecraft+NeoForge 产物
    $useManual = $false
    try {
        $probe = [System.Net.Sockets.TcpClient]::new()
        $task = $probe.ConnectAsync('maven.neoforged.net', 443)
        if (-not $task.Wait(5000) -or -not $probe.Connected) { $useManual = $true }
        $probe.Close()
    }
    catch { $useManual = $true }

    if ($env:AF400_FORCE_GRADLE -eq '1') { $useManual = $false }
    if ($env:AF400_FORCE_MANUAL -eq '1') { $useManual = $true }

    if ($useManual) {
        Write-Host "      maven.neoforged.net 不可达 -> 使用手工构建" -ForegroundColor Yellow
        $builtJar = & (Join-Path $PSScriptRoot 'build-manual.ps1') -Version $newVersion -ModRoot $ProjectRoot
        $builtJar = ($builtJar | Select-Object -Last 1)
        if (-not $builtJar -or -not (Test-Path $builtJar)) { throw "手工构建失败" }
        $built = $true
    }
    else {
        Write-Host "      使用 Gradle 构建" -ForegroundColor Gray
        Push-Location $ProjectRoot
        try {
            $gradlew = Join-Path $ProjectRoot 'gradlew.bat'
            if (-not (Test-Path $gradlew)) { throw "找不到 gradlew.bat" }
            & $gradlew build --no-daemon "-Pmod_version=$newVersion"
            if ($LASTEXITCODE -ne 0) {
                throw "构建失败（exit $LASTEXITCODE）。版本号已升到 $newVersion，修好后再跑一次 .\tools\build.ps1 -NoBump"
            }
            $built = $true
        }
        finally { Pop-Location }
    }
}
else {
    Write-Host "[2/5] -NoBuild：跳过构建" -ForegroundColor Yellow
}

# ---------- 3. 收集产物 ----------
Write-Host "[3/5] 收集产物到 dist\$newVersion\"
$libs = Join-Path $ProjectRoot 'build\libs'
$copied = @()
if ($builtJar) {
    $dest = Join-Path $jarOut (Split-Path $builtJar -Leaf)
    Copy-Item $builtJar $dest -Force
    $copied += Get-Item $dest
}
elseif ($built -and (Test-Path $libs)) {
    Get-ChildItem $libs -File -Filter *.jar |
        Where-Object { $_.Name -notmatch '-(sources|javadoc|dev|slim)\.jar$' } |
        ForEach-Object {
            $dest = Join-Path $jarOut $_.Name
            Copy-Item $_.FullName $dest -Force
            $copied += $_
        }
}
elseif ($built) {
    Write-Host "      警告：没有找到 build\libs 产物" -ForegroundColor Yellow
}

# ---------- 4. 写版本信息 ----------
Write-Host "[4/5] 写版本信息"
$info = [pscustomobject]@{
    version     = $newVersion
    previous    = $current
    changeKind  = $changeKind
    note        = $Note
    builtAt     = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
    built       = $built
    jars        = @($copied | ForEach-Object { $_.Name })
    sha256      = @($copied | ForEach-Object {
        [pscustomobject]@{
            file   = $_.Name
            sha256 = (Get-FileHash $_.FullName -Algorithm SHA256).Hash
        }
    })
}
$infoPath = Join-Path $jarOut 'version.json'
$info | ConvertTo-Json -Depth 6 |
    ForEach-Object { [System.IO.File]::WriteAllText($infoPath, $_, (New-Object System.Text.UTF8Encoding($false))) }

$logPath = Join-Path $jarOut 'CHANGELOG.md'
$lines = @(
    "# $newVersion",
    "",
    "- 上一版本：$($current)",
    "- 类型：$changeKind",
    "- 时间：$($info.builtAt)",
    "- 说明：$(if ($Note) { $Note } else { '（无）' })",
    "",
    "## 产物",
    ""
)
if ($copied.Count -gt 0) { foreach ($c in $copied) { $lines += "- ``$($c.Name)``" } }
else { $lines += "- （无，未构建）" }
[System.IO.File]::WriteAllLines($logPath, $lines, (New-Object System.Text.UTF8Encoding($false)))

# 记录版本历史
Add-VersionHistory -DistDir $DistDir -Version $newVersion -Kind $changeKind -Note $Note | Out-Null

# 刷新基线快照（下次靠它判断有没有加方块）
$blocks = Get-BlockRegistryNames $ProjectRoot
Save-BaselineBlocks -ProjectRoot $ProjectRoot -Blocks $blocks

# ---------- 5. 生成总清单 ----------
Write-Host "[5/5] 生成 dist\VERSIONS.md"
$jsonPath = Join-Path $DistDir '版本历史.json'
$history = @()
if (Test-Path $jsonPath) {
    $raw = Get-Content $jsonPath -Raw -Encoding UTF8
    if ($raw.Trim()) {
        $parsed = $raw | ConvertFrom-Json
        # 单个对象 / 数组 统一成数组，兼容只有一条记录的情况
        $history = if ($parsed -is [System.Array]) { @($parsed) } else { @($parsed) }
    }
}
$md = @(
    '# 400AF 版本存档',
    '',
    '所有构建版本都完整保留在本目录下，每版一个子文件夹。',
    '',
    '| 版本 | 类型 | 时间 | 说明 | 产物 |',
    '|---|---|---|---|---|'
)
foreach ($h in $history) {
    if ($null -eq $h) { continue }
    $md += "| $($h.Version) | $($h.Kind) | $($h.Timestamp) | $($h.Note) | $($h.Jar) |"
}
$md += @('', '> 由 `tools\build.ps1` 自动生成，勿手改。')
[System.IO.File]::WriteAllLines((Join-Path $DistDir 'VERSIONS.md'), $md, (New-Object System.Text.UTF8Encoding($false)))

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host " 完成：$newVersion" -ForegroundColor Green
Write-Host " 产物目录：dist\$newVersion\" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""

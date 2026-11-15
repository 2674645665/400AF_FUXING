# 启动 Willow11TH_Aero_beta2 并抓日志，用于验证 mod 是否正常加载
#
# 关键点：
#   1. 版本目录名含中文，用 -ArgumentList 数组传参并统一 UTF-8
#   2. natives 目录用 PCL 已生成的 "<版本名>-natives"
#   3. classpath / module-path 去重，排除 client-extra
param(
    [int]$TimeoutSeconds = 180,
    [switch]$KillAfter,
    [switch]$Headless
)

$ErrorActionPreference = 'Stop'
$mcRoot  = 'C:\Users\Administrator\Desktop\.minecraft'
$verName = 'Willow11TH_Aero_beta2(无地图)'
$verDir  = Join-Path "$mcRoot\versions" $verName
$json    = Get-Content (Join-Path $verDir "$verName.json") -Raw -Encoding UTF8 | ConvertFrom-Json

$javaExe = 'C:\Java\jdk-21\bin\java.exe'
$libDir  = Join-Path $mcRoot 'libraries'
$assets  = Join-Path $mcRoot 'assets'

# ---------- natives ----------
$natives = Join-Path "$mcRoot\versions" "$verName-natives"
if (-not (Test-Path $natives)) { New-Item -ItemType Directory $natives -Force | Out-Null }

# ---------- classpath ----------
$cpEntries = New-Object System.Collections.Generic.List[string]
foreach ($lib in $json.libraries) {
    $p = $null
    if ($lib.downloads.artifact.path) {
        $p = Join-Path $libDir ($lib.downloads.artifact.path -replace '/', '\')
    } elseif ($lib.name) {
        $parts = $lib.name -split ':'
        if ($parts.Count -ge 3) {
            $g = $parts[0] -replace '\.', '\'
            $a = $parts[1]; $v = $parts[2]
            $cls = if ($parts.Count -ge 4) { $parts[3] } else { '' }
            $file = if ($cls) { "$a-$v-$cls.jar" } else { "$a-$v.jar" }
            $p = Join-Path $libDir "$g\$a\$v\$file"
        }
    }
    if ($p -and (Test-Path $p)) { $cpEntries.Add($p) }
}
$verJar = Join-Path $verDir "$verName.jar"
if (Test-Path $verJar) { $cpEntries.Add($verJar) }

$cpEntries = @($cpEntries | Select-Object -Unique)
$cp = $cpEntries -join ';'

# ---------- 解析 jvm 参数 ----------
function Resolve-Tpl([string]$s) {
    return $s -replace '\$\{natives_directory\}', $natives `
             -replace '\$\{launcher_name\}', 'dsh' `
             -replace '\$\{launcher_version\}', '1.0' `
             -replace '\$\{classpath\}', $cp `
             -replace '\$\{classpath_separator\}', ';' `
             -replace '\$\{library_directory\}', $libDir `
             -replace '\$\{version_name\}', $verName `
             -replace '\$\{assets_index_name\}', $json.assetIndex.id
}

$argsList = New-Object System.Collections.Generic.List[string]
foreach ($a in $json.arguments.jvm) {
    $val = if ($a -is [string]) { $a } else { $a.value }
    if (-not $val) { continue }
    if ($val -eq '-XstartOnFirstThread') { continue }
    foreach ($piece in ($val -split '\s+(?=-)')) {
        if (-not $piece.Trim()) { continue }
        $argsList.Add((Resolve-Tpl $piece.Trim()))
    }
}

if ($Headless) { $argsList.Insert(0, '-Djava.awt.headless=true') }
$argsList.Insert(0, '-Dfile.encoding=UTF-8')
$argsList.Insert(0, '-Dsun.stdout.encoding=UTF-8')

$argsList.Add($json.mainClass)

$gameArgs = @(
    '--username', 'DevTester',
    '--version', $verName,
    '--gameDir', $verDir,
    '--assetsDir', $assets,
    '--assetIndex', $json.assetIndex.id,
    '--uuid', '00000000000000000000000000000001',
    '--accessToken', '0',
    '--userType', 'legacy',
    '--versionType', 'release',
    '--launchTarget', 'forgeclient',
    '--fml.mcVersion', '1.21.1',
    '--fml.fmlVersion', '4.0.43',
    '--fml.neoForgeVersion', '21.1.236',
    '--fml.neoFormVersion', '1.21.1-20240808.144430'
)
foreach ($g in $gameArgs) { $argsList.Add($g) }

$logFile = Join-Path $env:TEMP 'af400_game_test.log'
$errFile = Join-Path $env:TEMP 'af400_game_test.err'
Remove-Item $logFile, $errFile -Force -ErrorAction SilentlyContinue

Write-Host "classpath: $($cpEntries.Count) 个  |  natives: $natives"
Write-Host "启动 Minecraft……" -ForegroundColor Cyan

$proc = Start-Process -FilePath $javaExe -ArgumentList $argsList -WorkingDirectory $verDir -PassThru `
        -NoNewWindow -RedirectStandardOutput $logFile -RedirectStandardError $errFile

Write-Host "PID: $($proc.Id)  等待 $TimeoutSeconds 秒……"
$exited = $proc.WaitForExit($TimeoutSeconds * 1000)
if (-not $exited) {
    Write-Host "→ 仍在运行（未崩溃）" -ForegroundColor Green
    if ($KillAfter) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue; Write-Host "已结束进程" }
} else {
    Write-Host "→ 进程退出，退出码: $($proc.ExitCode)" -ForegroundColor Yellow
}

Write-Host "`n========== 错误输出（尾部 40 行）=========="
if (Test-Path $errFile) { Get-Content $errFile -Tail 40 }

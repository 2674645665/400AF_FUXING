# 手工构建 400AF（绕过 ModDevGradle）
#
# 为什么要这个：
#   本机 maven.neoforged.net 不可达，ModDevGradle 需要的 NFRT 1.0.5 拉不到。
#   但 ~/.gradle/caches/neoformruntime 里已经有完整的 compiledWithNeoForge 产物
#   （Minecraft 1.21.1 + NeoForge 的反混淆+编译结果），可以直接拿来做 classpath。
#
# 用法：pwsh -File tools/build-manual.ps1 -Version 0.0.1
param(
    [string]$Version = '',
    [string]$ModRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

. (Join-Path $PSScriptRoot 'version.ps1')

if (-not $Version) { $Version = Get-CurrentModVersion (Join-Path $ModRoot 'gradle.properties') }
Write-Host "构建版本: $Version" -ForegroundColor Cyan

# ---------- 1. 定位 Minecraft/NeoForge 编译产物 ----------
$nfrtCache = "$env:USERPROFILE\.gradle\caches\neoformruntime"
$compiled = Get-ChildItem $nfrtCache -Recurse -File -Filter 'compiledWithNeoForge_*_output.jar' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $compiled) {
    throw "找不到 compiledWithNeoForge 产物，无法手工构建。路径: $nfrtCache"
}
Write-Host "Minecraft+NeoForge 产物: $($compiled.Name) ($([math]::Round($compiled.Length/1MB,1)) MB)"

# ---------- 2. 组装 classpath ----------
$libs = [System.Collections.Generic.List[string]]::new()
$libs.Add($compiled.FullName)
foreach ($root in @(
        "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1",
        'C:\Users\Administrator\Desktop\.minecraft\libraries',
        "$env:USERPROFILE\.m2\repository")) {
    if (Test-Path $root) {
        Get-ChildItem $root -Recurse -File -Filter '*.jar' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch 'sources|javadoc' } |
            ForEach-Object { $libs.Add($_.FullName) }
    }
}
$libs = @($libs | Select-Object -Unique)

# 第三方 mod：从 mods 目录取 Create（含内嵌 Ponder / Flywheel / Registrate）
$modsDir = 'C:\Users\Administrator\Desktop\.minecraft\versions\Willow11TH_Aero_beta2(无地图)\mods'
if (Test-Path $modsDir) {
    Get-ChildItem $modsDir -File -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match 'create|ponder|flywheel|registrate|Hexie' } |
        ForEach-Object { $libs += $_.FullName }
}
# 解出来的 ponder 也要能用（catnip 在里面）
if (Test-Path "$env:TEMP\ponder.jar") { $libs += "$env:TEMP\ponder.jar" }
$libs = @($libs | Select-Object -Unique)
Write-Host "classpath: $($libs.Count) 个 jar"

# ---------- 3. 编译 ----------
$work = Join-Path $env:TEMP "af400_manual_build"
if (Test-Path $work) { Remove-Item $work -Recurse -Force }
$classesDir = Join-Path $work 'classes'
New-Item -ItemType Directory $classesDir -Force | Out-Null

$srcRoot = Join-Path $ModRoot 'src\main\java'
$javaFiles = @(Get-ChildItem $srcRoot -Recurse -File -Filter '*.java' | ForEach-Object { $_.FullName })
Write-Host "源文件: $($javaFiles.Count)"

$cpStr = ($libs | ForEach-Object { $_.Replace('\','/') }) -join ';'
$argFile = Join-Path $work 'javac_args.txt'
$args = @(
    '-proc:none', '-encoding UTF-8', '-nowarn', '-g',
    "-d `"$($classesDir.Replace('\','/'))`"",
    "-cp `"$cpStr`""
)
$args += $javaFiles | ForEach-Object { "`"$($_.Replace('\','/'))`"" }
[System.IO.File]::WriteAllLines($argFile, $args, (New-Object System.Text.UTF8Encoding($false)))

Write-Host "编译中……" -ForegroundColor Cyan
$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
if (-not (Test-Path $javac)) { $javac = 'javac' }

# 注意：不要把 stderr 和 stdout 混在一起判断成功与否。
# javac 的「警告」走 stderr，用 2>&1 会让 PowerShell 把警告当成错误，
# 导致 $LASTEXITCODE 判断失效、构建被误判为失败。
# 用临时 .bat 调 javac：
#   - 直接 & javac 时，stderr 的警告会被 PowerShell 记为错误，ExitCode 判断不可靠
#   - 写个 bat 能准确拿到 javac 的真实退出码
$javacOut = Join-Path $work 'javac_log.txt'
$javacBat = Join-Path $work 'do_javac.bat'
$batLines = @(
    '@echo off',
    ('"' + $javac + '" "@' + $argFile + '" > "' + $javacOut + '" 2>&1'),
    'exit /b %ERRORLEVEL%'
)
[System.IO.File]::WriteAllLines($javacBat, $batLines, (New-Object System.Text.UTF8Encoding($false)))

& cmd.exe /c $javacBat
$jpExit = $LASTEXITCODE

if ($jpExit -ne 0) {
    Write-Host "--- javac 输出 ---"
    Get-Content $javacOut -ErrorAction SilentlyContinue | Select-Object -First 40
    throw "编译失败（exit $jpExit）"
}
$warn = Get-Content $javacOut -ErrorAction SilentlyContinue | Where-Object { $_ -match '警告|warning|注:' }
if ($warn) {
    Write-Host "（javac 有提示，通常是 unchecked 泛型警告，不影响结果）" -ForegroundColor DarkGray
}
$classCount = (Get-ChildItem $classesDir -Recurse -File -Filter '*.class').Count
Write-Host "编译完成，$classCount 个 class" -ForegroundColor Green

# ---------- 4. 处理资源（模板变量替换）----------
$resRoot = Join-Path $ModRoot 'src\main\resources'
$props = @{}
Get-Content (Join-Path $ModRoot 'gradle.properties') -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^\s*([\w.]+)\s*=\s*(.*)$') { $props[$Matches[1]] = $Matches[2].Trim() }
}
$props['mod_version'] = $Version

function Expand-Props([string]$text) {
    # 替换 ${xxx} 形式
    return [regex]::Replace($text, '\$\{([\w.]+)\}', {
        param($m)
        $k = $m.Groups[1].Value
        if ($props.ContainsKey($k)) { return $props[$k] } else { return $m.Value }
    })
}

$resStage = Join-Path $work 'resources'
New-Item -ItemType Directory $resStage -Force | Out-Null
Get-ChildItem $resRoot -Recurse -File | Where-Object { $_.Name -notmatch '^_README' } | ForEach-Object {
    $rel = $_.FullName.Substring($resRoot.Length).TrimStart('\')
    $dest = Join-Path $resStage $rel
    New-Item -ItemType Directory (Split-Path $dest -Parent) -Force | Out-Null
    if ($_.Extension -in '.toml', '.json', '.mcmeta') {
        $txt = Get-Content $_.FullName -Raw -Encoding UTF8
        $txt = Expand-Props $txt
        [System.IO.File]::WriteAllText($dest, $txt, (New-Object System.Text.UTF8Encoding($false)))
    } else {
        Copy-Item $_.FullName $dest -Force
    }
}

# ---------- 5. 打 jar ----------
$fileNameVersion = $Version.Replace('_','-')
$jarName = "af400-1.21.1-neoforge-$fileNameVersion.jar"
$jarPath = Join-Path $work $jarName
if (Test-Path $jarPath) { Remove-Item $jarPath -Force }

$zip = [System.IO.Compression.ZipFile]::Open($jarPath, 'Create')
try {
    $addFile = {
        param($src, $entryName)
        $entry = $zip.CreateEntry($entryName.Replace('\','/'), [System.IO.Compression.CompressionLevel]::Optimal)
        $s = $entry.Open()
        $bytes = [System.IO.File]::ReadAllBytes($src)
        $s.Write($bytes, 0, $bytes.Length)
        $s.Dispose()
    }

    # class
    Get-ChildItem $classesDir -Recurse -File | ForEach-Object {
        & $addFile $_.FullName $_.FullName.Substring($classesDir.Length).TrimStart('\')
    }
    # 资源
    Get-ChildItem $resStage -Recurse -File | ForEach-Object {
        & $addFile $_.FullName $_.FullName.Substring($resStage.Length).TrimStart('\')
    }

    # MANIFEST
    $mf = @(
        'Manifest-Version: 1.0',
        "Specification-Title: $($props['mod_id'])",
        "Specification-Vendor: $($props['mod_authors'])",
        'Specification-Version: 1',
        "Implementation-Title: $($props['mod_name'])",
        "Implementation-Version: $Version",
        "Implementation-Vendor: $($props['mod_authors'])",
        ''
    ) -join "`r`n"
    $entry = $zip.CreateEntry('META-INF/MANIFEST.MF')
    $sw = New-Object System.IO.StreamWriter($entry.Open(), (New-Object System.Text.UTF8Encoding($false)))
    $sw.Write($mf); $sw.Dispose()
}
finally { $zip.Dispose() }

$size = [math]::Round((Get-Item $jarPath).Length / 1KB, 1)
Write-Host "打包完成: $jarName ($size KB)" -ForegroundColor Green
Write-Output (Join-Path $work $jarName)

# 把 Blockbench 导出的车厢板 OBJ 处理成 MC 可用的资源
#
# 做的事情（一次搞定，避免手工漏步骤）：
#   1. 平移模型：Blockbench 有时以中线建模（X 从 -0.5 开始），需要移到 0..N
#   2. 材质名统一改 t0（NeoForge OBJ 加载器按名字精确匹配）
#   3. 按窗户 UV 区域拆成 body / glass 两个 OBJ（composite 分层要两个文件）
#
# 用法：
#   .\tools\process-panel.ps1 -SourceDir "C:\...\车厢板" `
#       -SourceName "400AF宽客车板" -TargetName "af400_panel_wide_window" `
#       -WindowUMin 0 -WindowUMax 0.4531 -WindowVMin 0.3125 -WindowVMax 0.5664
param(
    [Parameter(Mandatory)][string]$SourceDir,
    [Parameter(Mandatory)][string]$SourceName,
    [Parameter(Mandatory)][string]$TargetName,
    [double]$WindowUMin = 0.0,
    [double]$WindowUMax = 0.1875,
    [double]$WindowVMin = 0.4375,
    [double]$WindowVMax = 0.5664,
    [switch]$NoShift,
    [string]$ModRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'

$partsDir = Join-Path $ModRoot "src\main\resources\assets\af400\models\block\af400\parts"
if (-not (Test-Path $partsDir)) { New-Item -ItemType Directory $partsDir -Force | Out-Null }

$srcObj = Join-Path $SourceDir "$SourceName.obj"
if (-not (Test-Path $srcObj)) { throw "找不到源 OBJ: $srcObj" }

$raw = Get-Content $srcObj -Encoding UTF8

# ---------- 1. 计算平移量 ----------
$xs = @()
foreach ($l in $raw) {
    if ($l -match '^v\s+(\S+)\s+(\S+)\s+(\S+)') { $xs += [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture) }
}
if ($xs.Count -eq 0) { throw "OBJ 里没有顶点" }
$minX = ($xs | Measure-Object -Minimum).Minimum
$maxX = ($xs | Measure-Object -Maximum).Maximum
$shiftX = 0.0
if (-not $NoShift -and [math]::Abs($minX) -gt 0.0001) {
    $shiftX = -$minX
    Write-Host "模型 X 范围 $minX..$maxX -> 平移 $([math]::Round($shiftX,4)) 使其从 0 开始"
} else {
    Write-Host "模型 X 范围 $minX..$maxX -> 无需平移"
}

# ---------- 2. 解析并重写 ----------
$vi = 0; $ti = 0
$vLines = @(); $vtLines = @(); $vnLines = @()
$vts = @{}
$faces = @()

foreach ($l in $raw) {
    if ($l -match '^v\s+(\S+)\s+(\S+)\s+(\S+)') {
        $x = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture) + $shiftX
        $y = $Matches[2]; $z = $Matches[3]
        $vLines += ('v {0} {1} {2}' -f $x.ToString('0.######', [Globalization.CultureInfo]::InvariantCulture), $y, $z)
    }
    elseif ($l -match '^vt\s+(\S+)\s+(\S+)') {
        $ti++
        $u = [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture)
        $v = [double]::Parse($Matches[2], [Globalization.CultureInfo]::InvariantCulture)
        $vts[$ti] = @($u, $v)
        $vtLines += $l
    }
    elseif ($l -match '^vn\s') { $vnLines += $l }
    elseif ($l -match '^f\s+(.+)') { $faces += $Matches[1] }
}

Write-Host "顶点 $($vLines.Count) / UV $($vtLines.Count) / 法线 $($vnLines.Count) / 面 $($faces.Count)"

# ---------- 3. 按窗户 UV 拆面 ----------
function Test-InWindow([string]$faceLine) {
    foreach ($p in ($faceLine -split '\s+')) {
        $seg = $p -split '/'
        if ($seg.Count -ge 2 -and $seg[1]) {
            $t = [int]$seg[1]
            if ($vts.ContainsKey($t)) {
                $u = $vts[$t][0]; $v = $vts[$t][1]
                if ($u -ge $WindowUMin -and $u -le $WindowUMax -and $v -ge $WindowVMin -and $v -le $WindowVMax) {
                    return $true
                }
            }
        }
    }
    return $false
}

$glassFaces = @(); $bodyFaces = @()
foreach ($f in $faces) {
    if (Test-InWindow $f) { $glassFaces += $f } else { $bodyFaces += $f }
}
Write-Host "主体面 $($bodyFaces.Count) / 窗户面 $($glassFaces.Count)"

if ($glassFaces.Count -eq 0) {
    Write-Warning "没有面落在窗户 UV 区域，glass 层会是空的。请检查 -WindowU*/V* 参数。"
}

# ---------- 4. 写文件 ----------
function Write-Part($path, $suffix, $faceLines) {
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("# 400AF $TargetName - $suffix")
    [void]$sb.AppendLine("# 由 tools/process-panel.ps1 生成，勿手改")
    [void]$sb.AppendLine("mtllib $TargetName.mtl")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("o ${TargetName}_$suffix")
    foreach ($l in $vLines)  { [void]$sb.AppendLine($l) }
    foreach ($l in $vtLines) { [void]$sb.AppendLine($l) }
    foreach ($l in $vnLines) { [void]$sb.AppendLine($l) }
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("usemtl t0")
    foreach ($f in $faceLines) { [void]$sb.AppendLine("f $f") }
    [System.IO.File]::WriteAllText($path, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))
}

Write-Part (Join-Path $partsDir "$TargetName.obj")       'full'  $faces
Write-Part (Join-Path $partsDir "${TargetName}_body.obj")  'body'  $bodyFaces
Write-Part (Join-Path $partsDir "${TargetName}_glass.obj") 'glass' $glassFaces

# MTL（t0 占位，实际贴图由模型 JSON 的 textures.t0 提供）
$mtl = @"
# 400AF $TargetName 材质
newmtl t0
map_Kd #t0
Kd 1 1 1
d 1
"@
[System.IO.File]::WriteAllText((Join-Path $partsDir "$TargetName.mtl"), $mtl, (New-Object System.Text.UTF8Encoding($false)))

Write-Host ""
Write-Host "已生成："
foreach ($n in @("$TargetName.obj", "${TargetName}_body.obj", "${TargetName}_glass.obj", "$TargetName.mtl")) {
    $p = Join-Path $partsDir $n
    "  {0,-45} {1} bytes" -f $n, (Get-Item $p).Length
}

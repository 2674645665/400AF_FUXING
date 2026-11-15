# 把 400AF 车厢板 OBJ 按「窗户 / 主体」拆成两个 OBJ，用于 composite 分层渲染
#
# 背景：窗户是半透明贴图（A=130），和完全不透明的车体混在一个模型里时，
#       minecraft:translucent 的渲染排序会出问题（背面看不见、深度错乱）。
#       拆成 body(cutout) + glass(translucent) 两个子模型就没有这个问题。
param(
    [string]$ModRoot = (Split-Path -Parent $PSScriptRoot),
    [double]$WindowUMin = 0.0,
    [double]$WindowUMax = 0.1875,
    [double]$WindowVMin = 0.4375,
    [double]$WindowVMax = 0.5664
)

$ErrorActionPreference = 'Stop'

$partsDir = Join-Path $ModRoot 'src\main\resources\assets\af400\models\block\af400\parts'
$srcObj = Join-Path $partsDir 'af400_panel_small_window.obj'
$lines = Get-Content $srcObj -Encoding UTF8

# ---------- 解析 OBJ ----------
$vts = @{}; $vtIdx = 0
$vn = @{}; $vnIdx = 0
$vLines = @()
$vtLines = @()
$vnLines = @()
$faces = @()

foreach ($line in $lines) {
    if ($line -match '^v\s+') { $vLines += $line }
    elseif ($line -match '^vt\s+(\S+)\s+(\S+)') {
        $vtIdx++; $vts[$vtIdx] = @([double]$Matches[1], [double]$Matches[2]); $vtLines += $line
    }
    elseif ($line -match '^vn\s+') { $vnIdx++; $vnLines += $line }
    elseif ($line -match '^f\s+(.+)') {
        $faces += $Matches[1]
    }
}

if ($faces.Count -eq 0) { throw "OBJ 里没有面数据" }

function Test-InWindow([string]$faceLine) {
    $parts = $faceLine -split '\s+'
    foreach ($p in $parts) {
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

# ---------- 写文件 ----------
function Write-Obj([string]$path, [string]$header, [string[]]$faceLines) {
    # 只保留被引用到的顶点/UV/法线会破坏索引，所以两份都写全量
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("# 400AF 车厢板 - $header")
    [void]$sb.AppendLine("# 由 tools/split-obj.ps1 生成，勿手改")
    [void]$sb.AppendLine("mtllib af400_panel_small_window.mtl")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("o af400_panel_small_window_$header")
    foreach ($l in $vLines)  { [void]$sb.AppendLine($l) }
    foreach ($l in $vtLines) { [void]$sb.AppendLine($l) }
    foreach ($l in $vnLines) { [void]$sb.AppendLine($l) }
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("usemtl t0")
    foreach ($f in $faceLines) { [void]$sb.AppendLine("f $f") }
    [System.IO.File]::WriteAllText($path, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))
}

Write-Obj (Join-Path $partsDir 'af400_panel_small_window_body.obj')  'body'  $bodyFaces
Write-Obj (Join-Path $partsDir 'af400_panel_small_window_glass.obj') 'glass' $glassFaces

Write-Host "主体面: $($bodyFaces.Count)  -> af400_panel_small_window_body.obj"
Write-Host "窗户面: $($glassFaces.Count)  -> af400_panel_small_window_glass.obj"
Write-Host "顶点 $($vLines.Count) / UV $($vtLines.Count) / 法线 $($vnLines.Count)（两份都全量保留）"

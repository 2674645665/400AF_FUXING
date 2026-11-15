# 为新增的车厢板批量生成资源骨架（blockstate / 掉落表 / parts 定义）
#
# 用法：
#   .\tools\gen-panel-resources.ps1 -Name af400_panel_no_window -Width 1 -Height 2
param(
    [Parameter(Mandatory)][string]$Name,
    [Parameter(Mandatory)][int]$WidthCells,
    [Parameter(Mandatory)][int]$HeightCells,
    [string]$ModRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
$res = Join-Path $ModRoot 'src\main\resources\assets\af400'
$data = Join-Path $ModRoot 'src\main\resources\data\af400'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$totalParts = $WidthCells * $HeightCells

# ---------- blockstate ----------
# 属性组合：facing(4) × part(16) × displays(7) = 448 个变体
#   displays 是 HeXie 的 PanelDisplayMount.PROPERTY（序列化名 displays），
#   HeXie 安装水牌时会直接 getValue() 它，缺了会崩游戏。
$facings = [ordered]@{ 'north' = 0; 'east' = 90; 'south' = 180; 'west' = 270 }
$displayMounts = @(
    'none',
    'number_left',
    'number_right',
    'sign_left',
    'sign_right',
    'number_left_sign_right',
    'sign_left_number_right'
)
$displayColumns = @('root', 'negative', 'positive')
$entries = @()
foreach ($f in $facings.Keys) {
    for ($p = 0; $p -lt 16; $p++) {
        $model = if ($p -eq 0) { "af400:block/af400/parts/$Name" } else { 'af400:block/af400/parts/empty' }
        foreach ($dm in $displayMounts) {
            foreach ($dc in $displayColumns) {
                $entries += "    `"facing=$f,part=$p,displays=$dm,display_column=$dc`": { `"model`": `"$model`", `"y`": $($facings[$f]) }"
            }
        }
    }
}
$bsJson = "{`r`n  `"variants`": {`r`n" + ($entries -join ",`r`n") + "`r`n  }`r`n}`r`n"
$bsPath = Join-Path $res "blockstates\$Name.json"
[System.IO.File]::WriteAllText($bsPath, $bsJson, $utf8)
Write-Host "blockstate: $($entries.Count) 个变体（4 朝向 x 16 part x 7 displays x 3 column）"

# ---------- 掉落表 ----------
$loot = @"
{
  "type": "minecraft:block",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "conditions": [
        { "condition": "minecraft:survives_explosion" }
      ],
      "entries": [
        { "type": "minecraft:item", "name": "af400:$Name" }
      ],
      "rolls": 1.0
    }
  ],
  "random_sequence": "af400:blocks/$Name"
}
"@
[System.IO.File]::WriteAllText((Join-Path $data "loot_table\blocks\$Name.json"), $loot, $utf8)
Write-Host "掉落表已写"

# ---------- parts 定义 ----------
$partsJson = @"
{
  "mount": [0, 0, 1],
  "side": true,
  "remote": false,
  "check_side": false,
  "bounds": [0.0, 0.0, 0.0375, $($WidthCells).0, $($HeightCells).0, 0.1875],
  "boxes": [
    [0, 0, 0.0375, $WidthCells, $HeightCells, 0.1875]
  ]
}
"@
[System.IO.File]::WriteAllText((Join-Path $data "af400_parts\$Name.json"), $partsJson, $utf8)
Write-Host "parts 定义已写（$WidthCells x $HeightCells，共 $totalParts 格）"

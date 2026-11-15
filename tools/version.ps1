# 400AF 版本引擎（被 bump.ps1 / build.ps1 / CI 共用）
#
# 版本规则（用户定义）：
#   1) 加方块  -> 第三位 +1，后缀重置    例：0.0.1 -> 0.0.2
#   2) 其他更新 -> 后缀递增              例：0.0.2_A0 -> 0.0.2_A1 -> ... -> 0.0.2_B0 -> 0.0.2_B1
#   3) 只有“加方块”会清掉后缀；A 段用尽（A9 之后）自动进 B0

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# 版本号正则：base = 0.0.X ，后缀 = _<大写字母><数字>
$script:VersionRegex = '^(?<base>\d+\.\d+\.\d+)(?:_(?<letter>[A-Z])(?<num>\d+))?$'

function Parse-AF400Version {
    param([Parameter(Mandatory)][string]$Version)

    # 用 -cmatch 大小写敏感：'_a0' 这类小写后缀必须拒绝
    if ($Version -cnotmatch $script:VersionRegex) {
        throw "版本号格式非法：'$Version'（应形如 0.0.2 或 0.0.2_A1）"
    }
    $base = $Matches['base']
    $letter = $Matches['letter']
    $num = $Matches['num']

    [pscustomobject]@{
        Raw     = $Version
        Base    = $base                     # 0.0.2
        Parts   = $base.Split('.') | ForEach-Object { [int]$_ }
        HasSuffix = [bool]$letter
        Letter  = if ($letter) { $letter } else { $null }
        Number  = if ($num) { [int]$num } else { $null }
    }
}

function Format-AF400Version {
    param(
        [Parameter(Mandatory)]$Parsed
    )
    if ($Parsed.HasSuffix) { return "$($Parsed.Base)_$($Parsed.Letter)$($Parsed.Number)" }
    return $Parsed.Base
}

<#
.SYNOPSIS
    计算下一个版本号。
.PARAMETER Kind
    'block' = 加方块（第三位+1，清后缀）
    'update' = 其他更新（后缀递增）
#>
function Get-NextAF400Version {
    param(
        [Parameter(Mandatory)][string]$Current,
        [Parameter(Mandatory)][ValidateSet('block', 'update')][string]$Kind
    )

    $p = Parse-AF400Version $Current

    if ($Kind -eq 'block') {
        $major = $p.Parts[0]
        $minor = $p.Parts[1]
        $patch = $p.Parts[2] + 1
        return [pscustomobject]@{
            Version = "$major.$minor.$patch"
            Kind    = $Kind
            From    = $Current
            Reason  = "加方块：$Current -> $major.$minor.$patch（后缀已重置）"
        }
    }

    # ---- update ----
    if (-not $p.HasSuffix) {
        # 裸版本 -> 开第一个 A0
        $next = "$($p.Base)_A0"
        return [pscustomobject]@{
            Version = $next
            Kind    = $Kind
            From    = $Current
            Reason  = "常规更新：$Current -> $next（开启 A 段）"
        }
    }

    $letter = $p.Letter
    $num = $p.Number + 1

    if ($num -gt 9) {
        # A 段用尽（A9 之后）-> 进下一个字母 B0
        $nextLetterCode = [int][char]$letter + 1
        if ($nextLetterCode -gt [int][char]'Z') {
            throw "版本后缀已用尽（超过 _Z9），请检查是否该改主版本号了"
        }
        $letter = [char]$nextLetterCode
        $num = 0
    }

    $next = "$($p.Base)_$($letter)$($num)"
    return [pscustomobject]@{
        Version = $next
        Kind    = $Kind
        From    = $Current
        Reason  = "常规更新：$Current -> $next"
    }
}

# ---------------------------------------------------------------------------
# 读取 / 写回 gradle.properties 里的 mod_version
# ---------------------------------------------------------------------------
function Get-CurrentModVersion {
    param([Parameter(Mandatory)][string]$GradlePropertiesPath)

    if (-not (Test-Path $GradlePropertiesPath)) {
        throw "找不到 gradle.properties：$GradlePropertiesPath"
    }
    $line = Select-String -Path $GradlePropertiesPath -Pattern '^\s*mod_version\s*=' -Encoding UTF8 |
            Select-Object -First 1
    if (-not $line) { throw "gradle.properties 里没有 mod_version" }
    return ($line.Line -replace '^\s*mod_version\s*=\s*', '').Trim()
}

function Set-ModVersion {
    param(
        [Parameter(Mandatory)][string]$GradlePropertiesPath,
        [Parameter(Mandatory)][string]$Version
    )
    $content = Get-Content $GradlePropertiesPath -Raw -Encoding UTF8
    $current = Get-CurrentModVersion $GradlePropertiesPath
    if ($current -eq $Version) {
        # 版本本来就一致（-NoBump 场景），无需写回
        return $Version
    }
    $new = [regex]::Replace($content, '(?m)^(\s*mod_version\s*=\s*).*$', "`${1}$Version")
    if ($new -eq $content) { throw "写回 mod_version 失败：未匹配到 mod_version 行" }
    # 无 BOM 写回
    [System.IO.File]::WriteAllText($GradlePropertiesPath, $new, (New-Object System.Text.UTF8Encoding($false)))
    return $Version
}

# ---------------------------------------------------------------------------
# 版本历史（dist/VERSIONS.md 的数据源：dist/版本历史.json）
# ---------------------------------------------------------------------------
function Add-VersionHistory {
    param(
        [Parameter(Mandatory)][string]$DistDir,
        [Parameter(Mandatory)][string]$Version,
        [Parameter(Mandatory)][string]$Kind,
        [string]$Note = ''
    )

    $jsonPath = Join-Path $DistDir '版本历史.json'
    $list = New-Object System.Collections.Generic.List[object]
    if (Test-Path $jsonPath) {
        $raw = Get-Content $jsonPath -Raw -Encoding UTF8
        if ($raw.Trim()) {
            $parsed = $raw | ConvertFrom-Json
            # ConvertFrom-Json 对单条记录返回对象而非数组，必须逐个展开
            foreach ($item in @($parsed)) { if ($null -ne $item) { $list.Add($item) } }
        }
    }

    $entry = [pscustomobject]@{
        Version   = $Version
        Kind      = $Kind
        Note      = $Note
        Timestamp = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
        Jar       = "af400-1.21.1-neoforge-$($Version.Replace('_','-')).jar"
    }
    $list.Add($entry)

    $json = ConvertTo-Json -InputObject $list.ToArray() -Depth 5
    [System.IO.File]::WriteAllText($jsonPath, $json, (New-Object System.Text.UTF8Encoding($false)))
    return $entry
}

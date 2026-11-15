# 判断本次改动属于「加方块」还是「普通更新」
#
# 判定依据（按优先级）：
#   A. 显式参数 -Kind block|update   （最高优先级，手动指定）
#   B. Git 工作区差异：有新增方块注册 -> block；否则 update
#   C. 非 Git 环境：比对上次存档的方块清单快照 (dist/.baseline/blocks.txt)
#      有新增方块 id -> block；否则 update
#
# “新增方块”的识别口径：
#   - src/main/resources/assets/af400/blockstates/**/*.json 的新增文件（方块必须有 blockstate）
#   - src/main/java 里 BlocksInit 及 init/block/** 新增的 register(...) 字符串名

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-BlockRegistryNames {
    param([Parameter(Mandatory)][string]$ProjectRoot)

    $names = New-Object System.Collections.Generic.HashSet[string]

    # 1) blockstates 文件名 = 方块 id（Minecraft 硬性要求）
    $bsDir = Join-Path $ProjectRoot 'src\main\resources\assets\af400\blockstates'
    if (Test-Path $bsDir) {
        Get-ChildItem $bsDir -Recurse -File -Filter *.json | ForEach-Object {
            $null = $names.Add($_.BaseName)
        }
    }

    # 2) Java 里注册的字面量方块名
    $initDir = Join-Path $ProjectRoot 'src\main\java\com\af400\af400\init'
    if (Test-Path $initDir) {
        Get-ChildItem $initDir -Recurse -File -Filter *.java | ForEach-Object {
            $text = Get-Content $_.FullName -Raw -Encoding UTF8
            foreach ($m in [regex]::Matches($text, 'BLOCKS\.register\w*\(\s*"([a-z0-9_]+)"')) {
                $null = $names.Add($m.Groups[1].Value)
            }
            foreach ($m in [regex]::Matches($text, '\.register\(\s*"([a-z0-9_]+)"\s*,\s*\(\)\s*->\s*new\s+\w*Block')) {
                $null = $names.Add($m.Groups[1].Value)
            }
        }
    }

    return @($names | Sort-Object)
}

function Get-GitChangedBlocks {
    param([Parameter(Mandatory)][string]$ProjectRoot)

    # 只在确实是 git 仓库时使用
    $gitDir = Join-Path $ProjectRoot '.git'
    if (-not (Test-Path $gitDir)) { return $null }

    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) { return $null }

    Push-Location $ProjectRoot
    try {
        $base = $null
        foreach ($cand in @('HEAD', 'HEAD~1')) {
            & git rev-parse --verify --quiet $cand *> $null
            if ($LASTEXITCODE -eq 0) { $base = $cand; break }
        }
        if (-not $base) { return $null }   # 还没有任何提交

        $status = & git status --porcelain
        if (-not $status) { return @() }

        $added = & git diff --name-status $base -- `
            'src/main/resources/assets/af400/blockstates' `
            'src/main/java/com/af400/af400/init' 2>$null

        $newBlocks = @()
        foreach ($line in $added) {
            if ($line -match '^A\s+(.+)$') {
                $f = $Matches[1]
                if ($f -match 'blockstates/(.+)\.json$') {
                    $newBlocks += $Matches[1]
                }
            }
        }
        return @($newBlocks | Sort-Object -Unique)
    }
    finally { Pop-Location }
}

function Get-BaselineBlocks {
    param([Parameter(Mandatory)][string]$ProjectRoot)

    $p = Join-Path $ProjectRoot 'dist\.baseline\blocks.txt'
    if (-not (Test-Path $p)) { return $null }
    $lines = Get-Content $p -Encoding UTF8 | Where-Object { $_.Trim() }
    return @($lines | ForEach-Object { $_.Trim() } | Sort-Object)
}

function Save-BaselineBlocks {
    param(
        [Parameter(Mandatory)][string]$ProjectRoot,
        [Parameter(Mandatory)][string[]]$Blocks
    )
    $dir = Join-Path $ProjectRoot 'dist\.baseline'
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory $dir -Force | Out-Null }
    $p = Join-Path $dir 'blocks.txt'
    [System.IO.File]::WriteAllLines($p, $Blocks, (New-Object System.Text.UTF8Encoding($false)))
}

<#
.SYNOPSIS
    判定本次改动的类型。
.OUTPUTS
    [pscustomobject] Kind='block'|'update', Reason, NewBlocks[]
#>
function Resolve-ChangeKind {
    param(
        [Parameter(Mandatory)][string]$ProjectRoot,
        [ValidateSet('auto', 'block', 'update')][string]$Kind = 'auto'
    )

    if ($Kind -ne 'auto') {
        return [pscustomobject]@{
            Kind      = $Kind
            Reason    = '由命令行显式指定'
            NewBlocks = @()
        }
    }

    $current = Get-BlockRegistryNames $ProjectRoot

    # 优先用 git 判定
    $gitNew = Get-GitChangedBlocks $ProjectRoot
    if ($null -ne $gitNew) {
        if ($gitNew.Count -gt 0) {
            return [pscustomobject]@{
                Kind      = 'block'
                Reason    = "git 检测到新增方块 $($gitNew.Count) 个：$($gitNew -join ', ')"
                NewBlocks = $gitNew
            }
        }
        return [pscustomobject]@{
            Kind      = 'update'
            Reason    = 'git 未检测到新增方块'
            NewBlocks = @()
        }
    }

    # 退回基线快照比对
    $baseline = Get-BaselineBlocks $ProjectRoot
    if ($null -eq $baseline) {
        return [pscustomobject]@{
            Kind      = 'update'
            Reason    = '无基线快照（首次运行），按普通更新处理'
            NewBlocks = @()
        }
    }

    $added = @($current | Where-Object { $baseline -notcontains $_ })
    if ($added.Count -gt 0) {
        return [pscustomobject]@{
            Kind      = 'block'
            Reason    = "快照比对发现新增方块 $($added.Count) 个：$($added -join ', ')"
            NewBlocks = $added
        }
    }
    return [pscustomobject]@{
        Kind      = 'update'
        Reason    = '快照比对未发现新增方块'
        NewBlocks = @()
    }
}

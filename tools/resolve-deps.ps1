# 自动补齐 NeoForge 依赖到本地 maven 仓库（~/.m2/repository），然后离线构建
# 用途：maven.neoforged.net 在本机网络不可达时，靠本地已缓存/已下载的依赖完成构建
$ProgressPreference = 'SilentlyContinue'
$dst      = "$env:USERPROFILE\.m2\repository"
$cacheSrc = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"
$mcLibs   = "C:\Users\Administrator\Desktop\.minecraft\libraries"
$mod      = "C:\Users\Administrator\Desktop\400AF-X\400AF"
$env:JAVA_HOME = "C:\Java\jdk-21"
$repos = @('https://repo1.maven.org/maven2','https://maven.fabricmc.net','https://libraries.minecraft.net')

function Add-Dep([string]$g, [string]$a, [string]$v) {
    $dstDir = Join-Path $dst "$($g -replace '\.','/')\$a\$v"
    New-Item -ItemType Directory $dstDir -Force | Out-Null

    foreach ($base in @((Join-Path $cacheSrc "$g\$a\$v"), (Join-Path $mcLibs "$($g -replace '\.','/')\$a\$v"))) {
        if (Test-Path $base) {
            Get-ChildItem $base -Recurse -File | ForEach-Object { Copy-Item $_.FullName (Join-Path $dstDir $_.Name) -Force }
            return 'local'
        }
    }
    $gp = $g -replace '\.','/'
    foreach ($repo in $repos) {
        try {
            Invoke-WebRequest -Uri "$repo/$gp/$a/$v/$a-$v.pom" -OutFile (Join-Path $dstDir "$a-$v.pom") -TimeoutSec 20 -UseBasicParsing
            try { Invoke-WebRequest -Uri "$repo/$gp/$a/$v/$a-$v.jar" -OutFile (Join-Path $dstDir "$a-$v.jar") -TimeoutSec 30 -UseBasicParsing } catch {}
            return 'download'
        } catch {}
    }
    return 'FAIL'
}

for ($i = 1; $i -le 15; $i++) {
    Write-Host "`n########## 第 $i 轮 ##########" -ForegroundColor Cyan
    $p = Start-Process -FilePath "$mod\gradlew.bat" -ArgumentList "build","--offline","--no-daemon","--console=plain" `
         -WorkingDirectory $mod -NoNewWindow -PassThru `
         -RedirectStandardOutput "$env:TEMP\loop_out.txt" -RedirectStandardError "$env:TEMP\loop_err.txt"
    $ok = $p.WaitForExit(300000)
    $code = if ($ok) { $p.ExitCode } else { -1 }
    Write-Host "退出码: $code"
    if ($code -eq 0) { Write-Host "==> 构建成功" -ForegroundColor Green; break }

    $err = Get-Content "$env:TEMP\loop_err.txt" -Raw -ErrorAction SilentlyContinue
    # 版本号里可能含 + 、- 等字符
    $missing = [regex]::Matches($err, 'Could not (?:resolve|find) ([\w.\-]+):([\w.\-]+):([\w.\-+]+)') |
        ForEach-Object { [pscustomobject]@{ G=$_.Groups[1].Value; A=$_.Groups[2].Value; V=$_.Groups[3].Value } } |
        Sort-Object G,A,V -Unique

    if (-not $missing -or @($missing).Count -eq 0) {
        Write-Host "没有更多可识别的缺失依赖，构建因其他原因失败：" -ForegroundColor Yellow
        Get-Content "$env:TEMP\loop_err.txt" -ErrorAction SilentlyContinue | Select-Object -Last 25
        break
    }
    Write-Host "缺失 $(@($missing).Count) 个，补齐中..."
    foreach ($m in $missing) {
        $r = Add-Dep $m.G $m.A $m.V
        $color = if ($r -eq 'FAIL') { 'Red' } else { 'Gray' }
        Write-Host ("  [{0,-8}] {1}:{2}:{3}" -f $r, $m.G, $m.A, $m.V) -ForegroundColor $color
    }
}

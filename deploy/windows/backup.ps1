<#
.SYNOPSIS
  桂航美食推荐排行榜 - 数据库与上传图片备份（Windows）

.DESCRIPTION
  读取 run.env.cmd 中的 DB_* 配置，导出 MySQL 数据库并压缩上传目录。
  密码只通过 MYSQL_PWD 环境变量传给 mysqldump，不出现在命令行参数中。

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File deploy\windows\backup.ps1
  powershell -ExecutionPolicy Bypass -File deploy\windows\backup.ps1 -BackupDir D:\backups -RetentionDays 30
#>
[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path $PSScriptRoot 'run.env.cmd'),
    [string]$BackupDir = 'C:\guatfood\backups',
    [int]$RetentionDays = 14,
    [string]$MysqldumpPath = '',
    [string]$UploadPath = ''
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "缺少环境文件: $EnvFile（请先复制 run.env.example.cmd 为 run.env.cmd）"
}

# 解析 run.env.cmd 中的 `set KEY=VALUE`
Get-Content -LiteralPath $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -match '^set\s+([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        $name = $matches[1]
        $value = $matches[2].Trim()
        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
             ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

foreach ($name in @('DB_NAME', 'DB_USERNAME', 'DB_PASSWORD')) {
    if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
        throw "环境变量 $name 未设置，请检查 $EnvFile"
    }
}

if (-not $env:DB_HOST) { $env:DB_HOST = '127.0.0.1' }
if (-not $env:DB_PORT) { $env:DB_PORT = '3306' }

if (-not $MysqldumpPath) {
    $cmd = Get-Command mysqldump -ErrorAction SilentlyContinue
    if ($cmd) {
        $MysqldumpPath = $cmd.Source
    } else {
        throw '未找到 mysqldump，请安装 MySQL 客户端，或用 -MysqldumpPath 指定完整路径'
    }
}

if ($RetentionDays -lt 1) { throw 'RetentionDays 必须大于 0' }

if (-not $UploadPath) {
    if ($env:UPLOAD_PATH) {
        $UploadPath = $env:UPLOAD_PATH
    } else {
        $UploadPath = Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) 'backend\uploads'
    }
}

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

# 备份包含用户数据与密码哈希：关闭 ACL 继承，仅允许 SYSTEM / Administrators 访问
try {
    & icacls $BackupDir /inheritance:r `
        /grant:r '*S-1-5-18:(OI)(CI)F' '*S-1-5-32-544:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "无法收紧备份目录 ACL，请手动检查权限：$BackupDir"
    }
} catch {
    Write-Warning "无法收紧备份目录 ACL：$($_.Exception.Message)"
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$sqlFile = Join-Path $BackupDir "guatfood-db-$stamp.sql"
$dbZip = "$sqlFile.zip"
$uploadZip = Join-Path $BackupDir "guatfood-uploads-$stamp.zip"

$env:MYSQL_PWD = $env:DB_PASSWORD
try {
    Write-Host "[1/3] 备份数据库 $env:DB_NAME ..."
    $dumpArgs = @(
        '--no-defaults',
        "--host=$env:DB_HOST",
        "--port=$env:DB_PORT",
        "--user=$env:DB_USERNAME",
        '--single-transaction',
        '--quick',
        '--routines',
        '--triggers',
        "--result-file=$sqlFile",
        '--databases',
        $env:DB_NAME
    )

    # MySQL 8 备份单库可能需要 PROCESS 权限；可用 --no-tablespaces 规避
    $helpText = (& $MysqldumpPath --no-defaults --help 2>&1 | Out-String)
    if ($helpText -match '--no-tablespaces') {
        $dumpArgs = @($dumpArgs[0], '--no-tablespaces') + $dumpArgs[1..($dumpArgs.Length - 1)]
    }

    & $MysqldumpPath @dumpArgs
    if ($LASTEXITCODE -ne 0) { throw "mysqldump 失败，退出码 $LASTEXITCODE" }
    if (-not (Test-Path -LiteralPath $sqlFile)) { throw 'mysqldump 未生成 SQL 文件' }

    Compress-Archive -LiteralPath $sqlFile -DestinationPath $dbZip -CompressionLevel Optimal -Force
    Remove-Item -LiteralPath $sqlFile -Force

    Write-Host "[2/3] 备份上传目录 $UploadPath ..."
    if (Test-Path -LiteralPath $UploadPath) {
        $items = Get-ChildItem -LiteralPath $UploadPath -Force -ErrorAction SilentlyContinue
        if ($items) {
            Compress-Archive -Path (Join-Path $UploadPath '*') -DestinationPath $uploadZip -CompressionLevel Optimal -Force
        } else {
            Write-Host '  上传目录为空，跳过图片备份'
        }
    } else {
        Write-Host '  上传目录不存在，跳过图片备份'
    }

    Write-Host "[3/3] 清理超过 $RetentionDays 天的旧备份 ..."
    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    Get-ChildItem -LiteralPath $BackupDir -File -Filter 'guatfood-*.zip' |
        Where-Object { $_.LastWriteTime -lt $cutoff } |
        Remove-Item -Force

    Write-Host '备份完成：'
    Get-ChildItem -LiteralPath $BackupDir -File -Filter 'guatfood-*.zip' |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 4 |
        ForEach-Object { Write-Host ('  {0}  {1:N1} MB' -f $_.Name, ($_.Length / 1MB)) }
}
finally {
    if (Test-Path -LiteralPath $sqlFile) { Remove-Item -LiteralPath $sqlFile -Force -ErrorAction SilentlyContinue }
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

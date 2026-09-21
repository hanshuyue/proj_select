[CmdletBinding()]
param(
    [string]$MavenRepository = "",
    [string]$Python = "python"
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
function Run([string]$exe, [string[]]$arguments) {
    & $exe @arguments
    if ($LASTEXITCODE -ne 0) { throw "$exe failed with exit code $LASTEXITCODE" }
}
foreach ($tool in @('npm.cmd', 'node', 'mvn.cmd', 'java', 'tar', $Python)) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "Missing tool: $tool" }
}
$nodeMajor = [int]((& node --version).TrimStart('v').Split('.')[0])
if ($nodeMajor -lt 22) { throw 'Node.js 22 or newer is required by the TypeScript tests.' }
$outputs = Join-Path $root 'outputs'
New-Item -ItemType Directory -Force -Path $outputs | Out-Null
$stage = Join-Path $outputs ('selectproject-stage-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage | Out-Null
$oldBase = $env:VITE_API_BASE_URL
try {
    $env:VITE_API_BASE_URL = '/api'
    Push-Location (Join-Path $root 'scaffold-vue')
    try {
        Run 'npm.cmd' @('ci', '--no-audit', '--no-fund')
        Run 'npm.cmd' @('run', 'test:unit')
        Run 'npm.cmd' @('run', 'build')
    } finally { Pop-Location }
    $mavenArgs = @('-B', '-pl', 'scaffold-admin', '-am', 'clean', 'package',
        '-Dtest=PageQueryTest,PageResultTest,RTest,*PptGenerationTest,InitiationPptRowInsertionTest,InitiationTemplateValidatorTest,ProductionMigrationIsolationTest,DashboardPrivacyTest,CollaborationRoleSecurityTest,RegisterRequestValidationTest,ProjectAccessSecurityTest',
        '-Dsurefire.failIfNoSpecifiedTests=false',
        ('-Ddict.template.path=' + (Join-Path $root 'deploy/templates/initiation/standard.pptx')),
        ('-Dselection.template.path=' + (Join-Path $root 'deploy/templates/selection/standard.pptx')))
    if ($MavenRepository) { $mavenArgs += '-Dmaven.repo.local=' + [IO.Path]::GetFullPath($MavenRepository) }
    Push-Location (Join-Path $root 'scaffold-system')
    try { Run 'mvn.cmd' $mavenArgs } finally { Pop-Location }
    New-Item -ItemType Directory -Path (Join-Path $stage 'app'),(Join-Path $stage 'web'),(Join-Path $stage 'deploy') | Out-Null
    Copy-Item -LiteralPath (Join-Path $root 'scaffold-system/scaffold-admin/target/gems-platform-server.jar') -Destination (Join-Path $stage 'app')
    Copy-Item -Path (Join-Path $root 'scaffold-vue/dist/*') -Destination (Join-Path $stage 'web') -Recurse
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'templates') -Destination $stage -Recurse
    # Explicit allowlist: no real env file, developer configuration, or business data.
    foreach ($name in @('install.sh','validate-release.py','database.sh','db-config.py','selectproject.env.example','selectproject.service','selectproject.conf','DATABASE.md')) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $stage 'deploy')
    }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'sql') -Destination (Join-Path $stage 'deploy') -Recurse
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README.md') -Destination (Join-Path $stage 'deploy/README.md')
    $commit = (& git -C $root rev-parse HEAD).Trim()
    [IO.File]::WriteAllText((Join-Path $stage 'deploy/build-info.txt'), "Source base commit: $commit`nIncludes current working tree changes.`nBuilt UTC: $([DateTime]::UtcNow.ToString('o'))`n", [Text.UTF8Encoding]::new($false))
    Run $Python @((Join-Path $PSScriptRoot 'source-manifest.py'), $root, (Join-Path $stage 'deploy/source-manifest.json'))
    Run $Python @((Join-Path $PSScriptRoot 'audit-package.py'), $stage)
    $temporaryArchive = Join-Path $outputs ('selectproject-' + [Guid]::NewGuid().ToString('N') + '.tar.gz')
    Run 'tar' @('-czf', $temporaryArchive, '-C', $stage, 'app', 'web', 'templates', 'deploy')
    Run 'tar' @('-tzf', $temporaryArchive)
    $archive = Join-Path $outputs 'selectproject-release.tar.gz'
    Move-Item -LiteralPath $temporaryArchive -Destination $archive -Force
    $checksum = (Get-FileHash -Algorithm SHA256 -LiteralPath $archive).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText(($archive + '.sha256'), "$checksum  selectproject-release.tar.gz`n", [Text.UTF8Encoding]::new($false))
    Write-Host "Release: $archive`nSHA256: $checksum`nStage retained for inspection: $stage"
} finally { $env:VITE_API_BASE_URL = $oldBase }

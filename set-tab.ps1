param([Parameter(Mandatory)][string]$ServerRoot)
$ErrorActionPreference = 'Stop'
$jar = Join-Path $PSScriptRoot 'dist/TAB-6.2.1.jar'
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $reader = [IO.StreamReader]::new($zip.GetEntry('config/config.yml').Open())
    $config = $reader.ReadToEnd()
    $reader.Dispose()
} finally {
    $zip.Dispose()
}

$custom = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'resources/tab-config.yml') -Raw
$section = '(?m)^(?<key>[a-z-]+):[^\r\n]*(?:\r?\n[ \t]+[^\r\n]*)*'
foreach ($override in [regex]::Matches($custom, $section)) {
    $key = [regex]::Escape($override.Groups['key'].Value)
    $pattern = [regex]::new('(?m)^' + $key + ':[^\r\n]*(?:\r?\n[ \t]+[^\r\n]*)*')
    if (!$pattern.IsMatch($config)) { throw "Unknown TAB section: $key" }
    $config = $pattern.Replace($config, $override.Value, 1)
}

$target = Join-Path $ServerRoot 'plugins/TAB'
New-Item -ItemType Directory -Force -Path $target | Out-Null
Set-Content -LiteralPath (Join-Path $target 'config.yml') -Value $config -Encoding utf8NoBOM
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources/tab-animations.yml') -Destination (Join-Path $target 'animations.yml') -Force

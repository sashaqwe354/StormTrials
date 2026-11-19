param([Parameter(Mandatory)][string]$PaperRoot)
$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$libraries = Join-Path $PaperRoot 'libraries'
$dependencies = @(Get-ChildItem -LiteralPath $libraries -Filter '*.jar' -File -Recurse)
if (!$dependencies.Count) { throw 'No Paper libraries found. Start the Paper test server first.' }
$classpath = $dependencies.FullName -join [IO.Path]::PathSeparator
$sources = @(Get-ChildItem -LiteralPath (Join-Path $project 'src') -Filter '*.java' -File)
$manifest = Join-Path $project 'resources/plugin.yml'
$versionLine = Select-String -LiteralPath $manifest -Pattern '^version:\s*([0-9]+\.[0-9]+\.[0-9]+)\s*$'
if (!$versionLine) { throw 'Plugin version is missing from plugin.yml.' }
$version = $versionLine.Matches[0].Groups[1].Value
$classes = Join-Path $project ('build/classes-' + [guid]::NewGuid())
$dist = Join-Path $project 'dist'
New-Item -ItemType Directory -Force -Path $classes,$dist | Out-Null
$compiler = (Get-Command javac -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
$jar = (Get-Command jar -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
& $compiler --release 21 -encoding UTF-8 -cp $classpath -d $classes @($sources.FullName)
if ($LASTEXITCODE) { throw 'Compilation failed.' }
$output = Join-Path $dist "StormTrials-$version.jar"
& $jar --create --file $output -C $classes . -C (Split-Path -Parent $manifest) plugin.yml
if ($LASTEXITCODE) { throw 'JAR packaging failed.' }
Write-Output $output

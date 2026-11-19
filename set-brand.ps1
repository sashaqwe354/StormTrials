param([Parameter(Mandatory)][string]$ServerRoot)
$ErrorActionPreference = 'Stop'
$file = Join-Path $ServerRoot 'server.properties'
$brand = '§b§lШтормовой архипелаг'
$details = '§7Стихийные бедствия §8· §f20 арен §8· §7Minecraft 1.21.11'
function Escape-PropertiesValue([string]$value) {
    $parts = foreach ($character in $value.ToCharArray()) {
        if ([int]$character -gt 127) { '\u' + ([int]$character).ToString('x4') }
        else { $character }
    }
    return ($parts -join '')
}
$motd = 'motd=' + (Escape-PropertiesValue $brand) + '\n' + (Escape-PropertiesValue $details)
$lines = Get-Content -LiteralPath $file
$result = $lines -replace '^motd=.*$', $motd
if (!$result.Where({ $_ -like 'motd=*' })) { $result += $motd }
Set-Content -LiteralPath $file -Value $result -Encoding ascii

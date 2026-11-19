param([Parameter(Mandatory)][string]$ClientJar, [Parameter(Mandatory)][string]$Output,
    [Parameter(Mandatory)][string]$Icon)
$ErrorActionPreference = 'Stop'
$recipes = [regex]::Matches((Get-Content -LiteralPath (Join-Path $PSScriptRoot 'src/Supplies.java') -Raw),
    '(?m)^\s*(?<key>[A-Z_]+)\(.*?Material\.(?<material>[A-Z_]+), Resource\.')
if ($recipes.Count -ne 28) { throw "Expected 28 gear definitions, got $($recipes.Count)" }

# Voxel boxes are x1,y1,z1,x2,y2,z2,texture-slot.
$designs = @{
    BANDAGE = @('white_wool','red_wool','light_gray_wool','3,5,4,13,11,12,0;5,5,3,7,11,13,2;8,5,3,10,11,13,2;6,11,6,10,12,10,1;7,11,5,9,12,11,1')
    TONIC = @('green_concrete','lime_concrete','copper_block','5,2,5,11,11,11,0;4,3,4,12,5,12,1;6,11,6,10,13,10,0;5,13,5,11,15,11,2')
    SPRINT = @('polished_blackstone','cyan_concrete','gold_block','3,4,5,12,9,11,0;5,9,5,10,13,11,1;9,10,5,14,12,11,1;11,8,5,15,10,11,2')
    SPRING = @('copper_block','iron_block','dark_prismarine','3,2,4,13,5,12,2;5,5,5,11,7,11,1;6,7,6,10,9,10,0;4,9,5,12,11,11,1;8,11,5,12,14,11,2')
    PARACHUTE = @('white_wool','orange_wool','dark_oak_planks','2,10,4,14,12,12,0;4,12,5,12,14,11,0;6,12,5,10,15,11,1;3,5,5,4,10,6,2;12,5,10,13,10,11,2;6,2,6,10,6,10,1')
    ARMOR = @('iron_block','deepslate_tiles','gold_block','3,3,6,13,13,10,0;2,4,6,5,12,10,1;11,4,6,14,12,10,1;6,4,5,10,11,6,2;5,6,5,11,9,6,2')
    BUFFER = @('amethyst_block','purple_concrete','iron_block','4,2,4,12,5,12,2;5,5,5,11,11,11,0;6,11,6,10,13,10,0;7,13,7,9,15,9,1;3,5,6,5,9,10,1;11,5,6,13,9,10,1')
    ANCHOR = @('deepslate_tiles','copper_block','gold_block','7,3,6,9,13,10,0;4,10,6,12,12,10,1;3,3,6,13,5,10,0;2,4,6,4,7,10,1;12,4,6,14,7,10,1;7,13,7,9,15,9,2')
    NEUTRALIZER = @('lime_concrete','green_concrete','iron_block','4,3,5,12,11,11,1;5,4,4,11,9,5,0;6,11,6,10,14,10,2;9,10,5,14,12,8,2;12,8,5,14,10,8,0')
    BLAST_PAD = @('red_wool','polished_blackstone','iron_block','2,4,4,14,11,12,1;3,5,3,13,10,4,0;5,7,2,11,8,3,2;7,5,2,9,10,3,2;2,3,4,14,4,12,2')
    BOOSTER = @('red_concrete','white_wool','gold_block','6,3,6,10,13,10,1;6,8,6,10,11,10,0;7,13,7,9,15,9,0;4,3,6,6,7,10,0;10,3,6,12,7,10,0;7,1,7,9,3,9,2')
    DASH = @('warped_planks','cyan_concrete','iron_block','3,5,5,12,11,11,0;9,6,4,13,10,12,2;13,7,6,15,9,10,1;1,6,6,3,10,10,1;5,11,7,9,13,9,2')
    ADRENALINE = @('red_concrete','gold_block','iron_block','5,3,5,11,10,11,2;6,4,4,10,9,5,0;7,10,7,9,14,9,2;4,13,7,12,15,9,1;4,2,6,12,3,10,0')
    FIELD_KIT = @('red_concrete','white_wool','dark_prismarine','2,3,4,14,11,12,0;5,11,6,11,13,10,2;4,6,3,12,8,4,1;7,4,3,9,10,4,1;2,3,11,14,4,13,2')
    RADAR = @('deepslate_tiles','lime_concrete','copper_block','3,3,5,13,12,11,0;4,5,4,11,11,5,1;5,7,3,6,9,4,2;9,6,3,10,7,4,2;11,12,7,12,15,9,2')
    SMOKE = @('polished_blackstone','light_gray_wool','iron_block','5,3,5,11,11,11,0;5,5,5,11,7,11,1;6,11,6,10,13,10,2;9,13,7,13,14,9,2;6,2,6,10,3,10,1')
    LODESTONE = @('polished_blackstone','gold_block','blue_ice','7,4,6,9,12,10,1;4,6,6,12,10,10,1;6,6,5,10,10,11,2;6,12,7,7,15,9,0;9,12,7,10,15,9,0;7,14,7,9,15,9,0')
    HOOD = @('warped_planks','black_wool','lime_concrete','3,7,4,13,13,12,0;4,4,4,6,10,12,0;10,4,4,12,10,12,0;6,6,9,10,10,12,1;6,9,3,10,11,5,2;4,4,5,12,5,11,1')
    CHARM = @('amethyst_block','crying_obsidian','gold_block','7,2,6,9,14,10,0;5,4,6,11,12,10,0;4,6,6,12,10,10,1;6,6,5,10,10,6,2;7,13,7,9,15,9,2')
    SATCHEL = @('brown_wool','copper_block','dark_oak_planks','3,3,4,13,11,12,0;3,9,3,13,12,9,2;5,4,3,7,10,4,1;9,4,3,11,10,4,1;5,12,6,11,14,10,2')
    ICE_COAT = @('blue_ice','light_blue_wool','iron_block','3,5,6,13,13,10,0;2,3,6,5,12,10,1;11,3,6,14,12,10,1;6,2,6,10,12,10,1;7,7,5,9,10,6,2')
    GROUNDING = @('copper_block','iron_block','gold_block','7,3,7,9,14,9,0;4,2,4,12,4,12,2;4,7,7,7,9,9,1;9,10,7,12,12,9,1;6,13,6,10,15,10,1')
    STABILIZER = @('deepslate_tiles','copper_block','yellow_concrete','2,3,5,14,8,11,0;4,8,5,12,11,11,1;6,11,6,10,13,10,2;3,4,4,6,7,5,2;10,4,4,13,7,5,2')
    SURGE = @('green_concrete','shroomlight','iron_block','6,2,5,10,12,11,0;4,4,5,12,9,11,0;6,5,4,10,9,5,1;6,12,6,10,14,10,2;7,14,7,9,15,9,1')
    SKYWALKER = @('purple_wool','light_blue_wool','iron_block','6,3,7,10,12,9,2;3,6,7,7,10,9,0;9,6,7,13,10,9,0;2,9,7,5,12,9,1;11,9,7,14,12,9,1;7,11,6,9,14,10,1')
    ICE_RUNE = @('blue_ice','light_blue_concrete','iron_block','5,3,6,11,13,10,0;3,6,6,13,10,10,0;7,4,5,9,12,6,1;5,7,5,11,9,6,1;7,13,7,9,15,9,2')
    STORM_RING = @('copper_block','gold_block','cyan_concrete','4,4,6,12,6,10,0;4,11,6,12,13,10,0;3,6,6,5,11,10,0;11,6,6,13,11,10,0;6,11,5,10,14,7,1;7,12,4,9,15,5,2')
    SHOCK_BOOTS = @('deepslate_tiles','cyan_concrete','iron_block','2,2,5,7,5,11,2;9,2,5,14,5,11,2;3,5,5,7,10,11,0;9,5,5,13,10,11,0;4,10,5,7,13,11,1;9,10,5,12,13,11,1')
}

$vanilla = [IO.Compression.ZipFile]::OpenRead($ClientJar)
$package = $null
$file = $null
function Read-VanillaJson([string]$name) {
    $entry = $vanilla.GetEntry($name)
    if (!$entry) { throw "Missing vanilla model: $name" }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { return ($reader.ReadToEnd() | ConvertFrom-Json -AsHashtable -Depth 100) }
    finally { $reader.Dispose() }
}
function Add-Json([string]$name, $value) {
    $entry = $package.CreateEntry($name, [IO.Compression.CompressionLevel]::Optimal)
    $writer = [IO.StreamWriter]::new($entry.Open(), [Text.UTF8Encoding]::new($false))
    try { $writer.Write(($value | ConvertTo-Json -Depth 100 -Compress)) }
    finally { $writer.Dispose() }
}

try {
    $version = Read-VanillaJson 'version.json'
    if ($version.id -ne '1.21.11') { throw "Expected Minecraft 1.21.11 client assets" }
    $major = $version.pack_version.resource_major
    $minor = $version.pack_version.resource_minor
    $file = [IO.File]::Create($Output)
    $package = [IO.Compression.ZipArchive]::new($file, [IO.Compression.ZipArchiveMode]::Create)
    Add-Json 'pack.mcmeta' @{ pack = @{ description = 'Штормовой архипелаг — модели снаряжения'; min_format = @($major,$minor); max_format = @($major,$minor) } }
    [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($package, $Icon, 'pack.png') | Out-Null
    $cases = @{}
    foreach ($match in $recipes) {
        $key = $match.Groups['key'].Value
        $material = $match.Groups['material'].Value.ToLowerInvariant()
        $name = $key.ToLowerInvariant()
        $style = $designs[$key]
        if (!$style) { throw "Missing geometry for $key" }
        $textures = @{ particle = 'minecraft:block/' + $style[0] }
        for ($i = 0; $i -lt 3; $i++) {
            if (!$vanilla.GetEntry("assets/minecraft/textures/block/$($style[$i]).png")) { throw "Texture not found: $($style[$i])" }
            $textures[[string]$i] = 'minecraft:block/' + $style[$i]
        }
        $elements = foreach ($shape in $style[3].Split(';')) {
            $v = $shape.Split(',') | ForEach-Object { [int]$_ }
            $faces = @{}
            foreach ($direction in 'down','up','north','south','west','east') { $faces[$direction] = @{ texture = '#' + $v[6] } }
            @{ from = @($v[0],$v[1],$v[2]); to = @($v[3],$v[4],$v[5]); faces = $faces }
        }
        $model = @{
            gui_light = 'front'; textures = $textures; elements = @($elements)
            display = @{
                gui = @{ rotation = @(20,35,0); scale = @(0.92,0.92,0.92) }
                ground = @{ translation = @(0,3,0); scale = @(0.48,0.48,0.48) }
                fixed = @{ scale = @(0.82,0.82,0.82) }
                firstperson_righthand = @{ rotation = @(0,25,0); scale = @(0.8,0.8,0.8) }
                thirdperson_righthand = @{ rotation = @(0,70,0); scale = @(0.55,0.55,0.55) }
            }
        }
        Add-Json "assets/stormtrials/models/item/$name.json" $model
        if (!$cases.ContainsKey($material)) { $cases[$material] = [Collections.Generic.List[object]]::new() }
        $cases[$material].Add(@{ when = "stormtrials:$name"; model = @{ type = 'minecraft:model'; model = "stormtrials:item/$name" } })
    }
    foreach ($material in $cases.Keys) {
        $path = "assets/minecraft/items/$material.json"
        $original = Read-VanillaJson $path
        $original.model = @{
            type = 'minecraft:select'; property = 'minecraft:custom_model_data'; index = 0
            cases = $cases[$material].ToArray(); fallback = $original.model
        }
        Add-Json $path $original
    }
} finally {
    if ($package) { $package.Dispose() }
    if ($file) { $file.Dispose() }
    $vanilla.Dispose()
}
"Packed $($recipes.Count) original models using vanilla fallbacks."

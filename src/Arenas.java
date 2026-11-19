package stormtrials;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.Vector;

final class Arenas {
    static final int SEA = 76;
    static final int PLATFORM = 80;
    static final int RADIUS = 34;
    static final int TOWER_Z = -62;
    static final int TOWER_TOP = 149;

    enum MapKind {
        PYRAMID("Храм Солнечного диска", 31),
        HARBOR("Порт Медной бухты", 28),
        OBSERVATORY("Полярная обсерватория", 28),
        FORT("Крепость четырёх ветров", 30),
        STATION("Станция последнего поезда", 30),
        GREENHOUSE("Оранжерея «Вега»", 30),
        QUARRY("Красный карьер", 30),
        VILLAGE("Деревня на сваях", 30),
        REACTOR("Геотермальный реактор", 30),
        MARKET("Ночной рынок", 29),
        LIBRARY("Архив приливов", 30),
        OILRIG("Буровая «Горизонт»", 30),
        RESERVOIR("Плотина облаков", 30),
        AIRFIELD("Аэродром Семи маяков", 30),
        MONASTERY("Монастырь над ущельем", 30),
        LIGHTHOUSE("Маяк Последнего рифа", 30),
        SHIP("Корабль «Громовая чайка»", 29),
        CITADEL("Стеклянная цитадель", 30),
        VOLCANO("Кальдера Искр", 30),
        SKYBRIDGE("Виадук Семи пролётов", 30);

        final String name;
        final int spawnZ;

        MapKind(String name, int spawnZ) {
            this.name = name;
            this.spawnZ = spawnZ;
        }
    }

    private record Position(int x, int y, int z) {}

    private final World world;
    private final Set<Position> structures = new HashSet<>();
    private final Set<Position> floodWater = new HashSet<>();
    private List<Position> structureSamples = List.of();
    private int floodLevel = SEA;
    private boolean trackStructure;

    Arenas(World world) {
        this.world = world;
    }

    static Location lobby(World world) {
        return new Location(world, 0.5, TOWER_TOP + 1, TOWER_Z + 4.5, 0, 12);
    }

    static Location spectator(World world) {
        return new Location(world, 0.5, 116, -34.5, 0, 28);
    }

    Location spawn(MapKind map, int index) {
        int offset = (index % 7 - 3) * 3;
        int floor = world.getHighestBlockYAt(offset, map.spawnZ);
        return new Location(world, offset + 0.5, floor + 1, map.spawnZ + 0.5, 180, 0);
    }

    void buildSite() {
        box(-RADIUS, 77, -RADIUS, RADIUS, 79, RADIUS, Material.DEEPSLATE_BRICKS);
        box(-RADIUS, PLATFORM, -RADIUS, RADIUS, PLATFORM, RADIUS, Material.SMOOTH_STONE);
        outline(-RADIUS, -RADIUS, RADIUS, RADIUS, PLATFORM, Material.POLISHED_BLACKSTONE);
        outline(-RADIUS + 1, -RADIUS + 1, RADIUS - 1, RADIUS - 1, PLATFORM, Material.YELLOW_TERRACOTTA);
        for (int i = -30; i <= 30; i += 5) {
            put(i, PLATFORM, -34, Material.SEA_LANTERN);
            put(i, PLATFORM, 34, Material.SEA_LANTERN);
            put(-34, PLATFORM, i, Material.SEA_LANTERN);
            put(34, PLATFORM, i, Material.SEA_LANTERN);
        }

        box(-2, TOWER_TOP, TOWER_Z + 2, 2, TOWER_TOP, TOWER_Z + 6, Material.SMOOTH_STONE);
    }

    void clearOldMap() {
        // Recover a partially damaged map on startup after an unclean shutdown.
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                int top = world.getHighestBlockYAt(x, z);
                for (int y = PLATFORM + 1; y <= Math.min(top, 116); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
        for (int x = -38; x <= 38; x++) {
            for (int z = -38; z <= 38; z++) {
                for (int y = SEA + 1; y <= 89; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.WATER) block.setType(Material.AIR, false);
                }
            }
        }
    }

    void load(MapKind map) {
        for (Position water : floodWater) {
            world.getBlockAt(water.x, water.y, water.z).setType(Material.AIR, false);
        }
        floodWater.clear();
        floodLevel = SEA;
        for (Position old : structures) {
            world.getBlockAt(old.x, old.y, old.z).setType(Material.AIR, false);
        }
        structures.clear();
        trackStructure = true;
        try {
            switch (map) {
                case PYRAMID -> pyramid();
                case HARBOR -> harbor();
                case OBSERVATORY -> observatory();
                case FORT -> fort();
                case STATION -> station();
                case GREENHOUSE -> greenhouse();
                case QUARRY -> quarry();
                case VILLAGE -> village();
                case REACTOR -> reactor();
                case MARKET -> market();
                case LIBRARY -> library();
                case OILRIG -> oilRig();
                case RESERVOIR -> reservoir();
                case AIRFIELD -> airfield();
                case MONASTERY -> monastery();
                case LIGHTHOUSE -> lighthouse();
                case SHIP -> ship();
                case CITADEL -> citadel();
                case VOLCANO -> volcano();
                case SKYBRIDGE -> skybridge();
            }
            detail(map);
        } finally {
            trackStructure = false;
        }
        structureSamples = List.copyOf(structures);
    }

    void raiseWater(int level) {
        while (floodLevel < level) {
            floodLevel++;
            int edge = floodLevel <= PLATFORM ? 38 : 36;
            for (int x = -edge; x <= edge; x++) {
                for (int z = -edge; z <= edge; z++) {
                    Block block = world.getBlockAt(x, floodLevel, z);
                    if (block.getType().isAir()) placeWater(block);
                }
            }
        }
    }

    Location findExposedStructure(Random random, int minimumY, int maximumY) {
        for (int i = 0; i < 24; i++) {
            int x = random.nextInt(-28, 29);
            int z = random.nextInt(-28, 29);
            int top = world.getHighestBlockYAt(x, z);
            if (top >= minimumY && top <= maximumY && structures.contains(new Position(x, top, z))) {
                return new Location(world, x + 0.5, top + 0.5, z + 0.5);
            }
        }
        return null;
    }

    Location findFloodContact(Random random) {
        if (floodLevel <= PLATFORM || structureSamples.isEmpty()) return null;
        for (int attempt = 0; attempt < 180; attempt++) {
            Position point = structureSamples.get(random.nextInt(structureSamples.size()));
            if (touchesFlood(point)) return new Location(world, point.x + 0.5, point.y + 0.55, point.z + 0.5);
        }
        return null;
    }

    private boolean touchesFlood(Position point) {
        if (point.y > floodLevel) return false;
        Block block = world.getBlockAt(point.x, point.y, point.z);
        if (!block.getType().isSolid()) return false;
        return world.getBlockAt(point.x + 1, point.y, point.z).getType() == Material.WATER
            || world.getBlockAt(point.x - 1, point.y, point.z).getType() == Material.WATER
            || world.getBlockAt(point.x, point.y, point.z + 1).getType() == Material.WATER
            || world.getBlockAt(point.x, point.y, point.z - 1).getType() == Material.WATER;
    }

    enum Damage { BLAST, WIND, ACID, WATER }

    int damageSphere(Location center, int radius, int limit, Damage damage) {
        List<Position> blocks = new ArrayList<>();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        Position point = new Position(cx + dx, cy + dy, cz + dz);
                        if (point.y > PLATFORM && structures.contains(point)
                            && (damage != Damage.WATER || touchesFlood(point))
                            && (damage != Damage.ACID || world.getHighestBlockYAt(point.x, point.z) == point.y)) {
                            blocks.add(point);
                        }
                    }
                }
            }
        }
        blocks.sort(Comparator.comparingDouble(p ->
            (p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy) + (p.z - cz) * (p.z - cz)));
        int broken = 0;
        for (Position position : blocks) {
            if (broken >= limit) break;
            Block block = world.getBlockAt(position.x, position.y, position.z);
            if (block.getType().isAir() || block.getType() == Material.WATER) continue;
            BlockData material = block.getBlockData();
            if (position.y <= floodLevel) placeWater(block);
            else block.setType(Material.AIR, false);
            if (broken < 3) {
                Location debris = block.getLocation().add(0.5, 0.1, 0.5);
                if (position.y > floodLevel) {
                    var piece = world.spawnFallingBlock(debris, material);
                    piece.setPersistent(false);
                    piece.setDropItem(false);
                    piece.setHurtEntities(false);
                    double vx = (position.x + 0.5 - center.getX()) * 0.13;
                    double vz = (position.z + 0.5 - center.getZ()) * 0.13;
                    piece.setVelocity(new Vector(vx + (broken - 1) * 0.12, 0.25 + broken * 0.06, vz));
                    org.bukkit.Bukkit.getScheduler().runTaskLater(
                        org.bukkit.plugin.java.JavaPlugin.getPlugin(StormTrials.class), piece::remove, 36L);
                }
                world.spawnParticle(Particle.BLOCK, debris, 12, 0.35, 0.35, 0.35, 0.03, material);
            }
            broken++;
        }
        if (broken > 0) world.playSound(center, Sound.BLOCK_STONE_BREAK, 0.55f, 0.75f);
        return broken;
    }

    private void placeWater(Block block) {
        block.setType(Material.WATER, false);
        floodWater.add(new Position(block.getX(), block.getY(), block.getZ()));
    }

    private void pyramid() {
        int centerZ = -3;
        for (int level = 0; level < 18; level++) {
            int width = 27 - level;
            int y = PLATFORM + level + 1;
            Material shell = level % 4 == 0 ? Material.CUT_SANDSTONE : Material.SMOOTH_SANDSTONE;
            outline(-width, centerZ - width, width, centerZ + width, y, shell);
            outline(1 - width, centerZ - width + 1, width - 1, centerZ + width - 1, y, Material.SANDSTONE);
            if (level < 5) {
                box(-2, y, centerZ - width, 2, y, centerZ - width + 1, Material.AIR);
                box(-2, y, centerZ + width - 1, 2, y, centerZ + width, Material.AIR);
                box(-width, y, centerZ - 2, 1 - width, y, centerZ + 2, Material.AIR);
                box(width - 1, y, centerZ - 2, width, y, centerZ + 2, Material.AIR);
            }
        }
        box(-10, 98, -13, 10, 98, 7, Material.SMOOTH_SANDSTONE);
        box(-5, 99, -8, 5, 99, 2, Material.GOLD_BLOCK);
        box(-2, 100, -5, 2, 100, -1, Material.WAXED_COPPER_BLOCK);
        put(0, 101, -3, Material.SEA_LANTERN);
        for (int step = 0; step < 18; step++) {
            int z = 24 - step;
            box(-3, 86, z, 3, 86 + step, z, Material.AIR);
            box(-3, 81 + step, z, 3, 81 + step, z, Material.CHISELED_SANDSTONE);
            for (int side : new int[]{-1, 1}) put(side * 4, 82 + step, z, Material.CUT_SANDSTONE);
        }
        box(-2, 81, -27, 2, 81, 33, Material.ORANGE_TERRACOTTA);
        box(-29, 81, -4, 29, 81, -2, Material.CUT_SANDSTONE);
        for (int x : new int[]{-16, -8, 8, 16}) {
            for (int z : new int[]{-19, -11, 5, 13}) {
                box(x, 81, z, x, 86, z, Material.CHISELED_SANDSTONE);
                put(x, 87, z, Material.SHROOMLIGHT);
            }
        }
        for (int side : new int[]{-1, 1}) {
            int x = 12 * side;
            box(x - 2, 81, 28, x + 2, 81, 32, Material.CUT_SANDSTONE);
            box(x - 1, 82, 29, x + 1, 89, 31, Material.SMOOTH_RED_SANDSTONE);
            put(x, 90, 30, Material.GOLD_BLOCK);
        }
    }

    private void harbor() {
        for (int side : new int[]{-1, 1}) {
            int x = side * 17;
            box(x - 10, 81, -26, x + 10, 81, 23, Material.DARK_OAK_PLANKS);
            shed(x, -13, 8, 10, Material.BRICKS, Material.WAXED_WEATHERED_COPPER, Material.ORANGE_STAINED_GLASS);
            shed(x, 13, 7, 7, Material.STRIPPED_SPRUCE_WOOD, Material.WAXED_EXPOSED_COPPER, Material.YELLOW_STAINED_GLASS);
            for (int z : new int[]{-28, 23}) {
                box(x - 7, 82, z, x - 5, 85, z + 2, Material.TERRACOTTA);
                box(x + 5, 82, z, x + 8, 84, z + 2, Material.CYAN_TERRACOTTA);
            }
        }
        box(-3, 81, -21, 3, 81, 20, Material.POLISHED_ANDESITE);
        for (int side : new int[]{-1, 1}) {
            int x = side * 5;
            for (int z : new int[]{-24, 0}) {
                box(x - 1, 81, z - 1, x + 1, 99, z + 1, Material.YELLOW_CONCRETE);
                box(x - 5, 99, z, x + 5, 100, z, Material.YELLOW_CONCRETE);
                box(-1, 91, z, 1, 99, z, Material.IRON_BARS);
                box(-2, 91, z - 1, 2, 91, z + 1, Material.GRAY_CONCRETE);
            }
        }
        stairs(-6, 6, 16, -1, 7, 81, Material.DARK_OAK_PLANKS);
        box(-8, 88, -12, 8, 88, 9, Material.WAXED_COPPER_BLOCK);
        for (int x : new int[]{-8, 8}) {
            for (int z : new int[]{-12, 9}) box(x, 82, z, x, 88, z, Material.SPRUCE_LOG);
        }
        outline(-8, -12, 8, 9, 89, Material.ORANGE_STAINED_GLASS);
    }

    private void observatory() {
        box(-27, 81, -26, -10, 86, 8, Material.CALCITE);
        box(10, 81, -23, 27, 85, 14, Material.SNOW_BLOCK);
        stairs(-20, -16, 15, -1, 7, 81, Material.CALCITE);
        stairs(15, 20, 20, -1, 6, 81, Material.PACKED_ICE);
        shed(-19, -11, 7, 8, Material.POLISHED_DIORITE, Material.DARK_PRISMARINE, Material.LIGHT_BLUE_STAINED_GLASS, 86);
        shed(18, -7, 7, 8, Material.POLISHED_ANDESITE, Material.WAXED_OXIDIZED_COPPER, Material.BLUE_STAINED_GLASS, 85);
        box(-9, 86, -14, 9, 86, -8, Material.POLISHED_ANDESITE);
        outline(-9, -14, 9, -8, 87, Material.LIGHT_BLUE_STAINED_GLASS);
        box(-7, 81, -5, 7, 81, 13, Material.WHITE_CONCRETE);
        for (int x : new int[]{-6, 6}) {
            for (int z : new int[]{-18, -6}) box(x, 82, z, x, 98, z, Material.QUARTZ_PILLAR);
        }
        box(-8, 98, -20, 8, 98, -4, Material.SMOOTH_QUARTZ);
        for (int y = 0; y < 6; y++) {
            int radius = 8 - y;
            outline(-radius, -12 - radius, radius, -12 + radius, 99 + y, Material.BLUE_STAINED_GLASS);
        }
        box(-3, 104, -15, 3, 104, -9, Material.GRAY_CONCRETE);
        box(0, 105, -12, 0, 111, -12, Material.IRON_BLOCK);
        put(0, 112, -12, Material.SEA_LANTERN);
        for (int x : new int[]{-24, -8, 8, 24}) {
            box(x, 81, 22, x, 84, 22, Material.POLISHED_DEEPSLATE);
            put(x, 85, 22, Material.SEA_LANTERN);
        }
    }

    private void fort() {
        Material stone = Material.STONE_BRICKS;
        for (int x : new int[]{-22, 22}) {
            for (int z : new int[]{-21, 17}) {
                tower(x, z, 5, 93, stone, Material.MOSSY_STONE_BRICKS);
            }
            box(x - 1, 81, -17, x + 1, 87, 13, stone);
            for (int z = -16; z <= 12; z += 4) box(x - 1, 88, z, x + 1, 89, z + 1, stone);
        }
        for (int side : new int[]{-1, 1}) {
            box(side * 10 - 7, 81, -22, side * 10 + 7, 87, -20, stone);
            box(side * 11 - 7, 81, 16, side * 11 + 7, 87, 18, stone);
        }
        shed(0, -6, 9, 7, Material.POLISHED_ANDESITE, Material.DARK_PRISMARINE, Material.GREEN_STAINED_GLASS);
        stairs(-18, -16, 10, -1, 7, 81, stone);
        box(-21, 87, 2, -16, 87, 4, stone);
        box(-22, 87, -18, -18, 87, 15, stone);
    }

    private void station() {
        for (int x : new int[]{-11, 0, 11}) {
            box(x - 2, 81, -27, x + 2, 81, 26, Material.GRAY_CONCRETE);
            for (int z = -26; z < 27; z += 3) box(x - 2, 82, z, x + 2, 82, z, Material.DARK_OAK_PLANKS);
            box(x - 1, 82, -27, x - 1, 82, 26, Material.IRON_BLOCK);
            box(x + 1, 82, -27, x + 1, 82, 26, Material.IRON_BLOCK);
        }
        for (int z : new int[]{-19, -6, 7}) {
            shed(-11, z, 3, 5, Material.RED_CONCRETE, Material.DEEPSLATE_TILES, Material.WHITE_STAINED_GLASS, 82);
        }
        for (int x : new int[]{-23, 22}) {
            box(x - 4, 81, -23, x + 4, 84, 22, Material.POLISHED_ANDESITE);
            stairs(x - 2, x + 2, 27, -1, 4, 81, Material.POLISHED_ANDESITE);
            for (int z = -17; z <= 17; z += 8) box(x, 85, z, x, 92, z, Material.IRON_BLOCK);
        }
        box(-27, 92, -20, 26, 92, 0, Material.BLUE_STAINED_GLASS);
        box(-24, 93, -17, 24, 93, -3, Material.POLISHED_DEEPSLATE);
        shed(21, 13, 6, 7, Material.BRICKS, Material.WAXED_COPPER_BLOCK, Material.LIGHT_GRAY_STAINED_GLASS, 84);
    }

    private void greenhouse() {
        for (int x : new int[]{-15, 15}) {
            shed(x, -6, 11, 19, Material.LIME_STAINED_GLASS, Material.GREEN_STAINED_GLASS, Material.GLASS);
            for (int z = -20; z <= 10; z += 7) {
                box(x - 6, 82, z, x - 3, 83, z + 3, Material.MOSS_BLOCK);
                box(x + 3, 82, z, x + 6, 83, z + 3, Material.AZALEA_LEAVES);
                put(x, 86, z, Material.SHROOMLIGHT);
            }
        }
        stairs(-3, 3, 20, -1, 8, 81, Material.WARPED_PLANKS);
        box(-5, 88, -17, 5, 88, 12, Material.WARPED_PLANKS);
        for (int z = -15; z < 12; z += 8) box(-5, 81, z, -5, 88, z, Material.COPPER_BLOCK);
        box(-6, 93, -15, 6, 93, 7, Material.WAXED_WEATHERED_COPPER);
    }

    private void quarry() {
        Material[] bands = {Material.RED_SANDSTONE, Material.ORANGE_TERRACOTTA, Material.TERRACOTTA, Material.BROWN_TERRACOTTA};
        for (int step = 0; step < 7; step++) {
            for (int side : new int[]{-1, 1}) {
                int x = (23 + step / 2) * side;
                box(x - 8 + step, 81 + step, -23, x + 8 - step, 81 + step, 14, bands[step % bands.length]);
            }
            box(-15 + step, 81 + step, -29, 15 - step, 81 + step, -18 - step, bands[step % bands.length]);
        }
        stairs(-23, -19, 21, -1, 8, 81, Material.CUT_RED_SANDSTONE);
        shed(17, -10, 6, 8, Material.STRIPPED_ACACIA_WOOD, Material.WAXED_COPPER_BLOCK, Material.ORANGE_STAINED_GLASS, 87);
        box(-23, 92, -11, 17, 92, -7, Material.DARK_OAK_PLANKS);
        for (int x = -19; x <= 15; x += 8) box(x, 81, -9, x, 92, -9, Material.STRIPPED_DARK_OAK_WOOD);
    }

    private void village() {
        for (int x : new int[]{-18, 0, 18}) {
            for (int z : new int[]{-17, 5}) {
                raisedHouse(x, z, 6, 6, Material.SPRUCE_PLANKS, x == 0 ? Material.DARK_PRISMARINE : Material.DARK_OAK_PLANKS);
                stairs(x - 2, x + 2, z + 11, -1, 5, 81, Material.SPRUCE_PLANKS);
            }
        }
        box(-25, 85, -12, 25, 85, -9, Material.SPRUCE_PLANKS);
        box(-25, 85, 10, 25, 85, 13, Material.SPRUCE_PLANKS);
        box(-2, 85, -10, 2, 85, 11, Material.BIRCH_PLANKS);
        for (int x : new int[]{-26, 26}) box(x, 81, 17, x, 92, 17, Material.SPRUCE_LOG);
    }

    private void reactor() {
        for (int step = 0; step < 5; step++) {
            int edge = 19 - step;
            outline(-edge, -edge, edge, edge, 81 + step, Material.DEEPSLATE_TILES);
        }
        for (int x : new int[]{-12, 12}) {
            tower(x, -8, 5, 104, Material.SMOOTH_STONE, Material.CYAN_STAINED_GLASS);
            box(x - 3, 100, -11, x + 3, 100, -5, Material.SEA_LANTERN);
            box(x - 2, 105, -10, x + 2, 108, -6, Material.WAXED_OXIDIZED_COPPER);
        }
        box(-8, 90, -9, 8, 90, -5, Material.CUT_COPPER);
        shed(0, 13, 11, 7, Material.LIGHT_GRAY_CONCRETE, Material.POLISHED_DEEPSLATE, Material.YELLOW_STAINED_GLASS);
        stairs(17, 20, 20, -1, 11, 81, Material.IRON_BLOCK);
        box(12, 91, 1, 20, 91, 10, Material.IRON_BLOCK);
        for (int x : new int[]{-27, 27}) box(x, 81, -25, x + 1, 88, 23, Material.ORANGE_CONCRETE);
    }

    private void market() {
        Material[] roofs = {Material.RED_WOOL, Material.ORANGE_WOOL, Material.LIGHT_BLUE_WOOL, Material.PURPLE_WOOL};
        for (int lane = 0; lane < 4; lane++) {
            int x = -24 + 16 * lane;
            for (int z = -21; z <= 17; z += 12) {
                box(x - 4, 81, z - 3, x + 4, 81, z + 3, Material.CUT_SANDSTONE);
                box(x - 4, 85, z - 4, x + 4, 85, z + 4, roofs[lane]);
                for (int side : new int[]{-1, 1}) {
                    box(x + side * 3, 82, z - 3, x + side * 3, 85, z - 3, Material.BAMBOO_BLOCK);
                    box(x + side * 3, 82, z + 3, x + side * 3, 85, z + 3, Material.BAMBOO_BLOCK);
                }
                put(x, 84, z, Material.SHROOMLIGHT);
            }
        }
        box(-10, 90, -8, 10, 90, 8, Material.MANGROVE_PLANKS);
        stairs(-3, 3, 18, -1, 10, 81, Material.MANGROVE_PLANKS);
        outline(-10, -8, 10, 8, 91, Material.MANGROVE_FENCE);
    }

    private void library() {
        shed(0, -6, 23, 19, Material.BRICKS, Material.DEEPSLATE_BRICKS, Material.YELLOW_STAINED_GLASS);
        for (int x : new int[]{-18, -12, -6, 6, 12, 18}) {
            box(x, 81, -21, x, 84, 4, Material.BOOKSHELF);
            box(x, 85, -21, x, 85, 4, Material.DARK_OAK_PLANKS);
        }
        box(-21, 86, -20, 21, 86, -15, Material.DARK_OAK_PLANKS);
        stairs(-20, -17, 12, -1, 6, 81, Material.DARK_OAK_PLANKS);
        box(-21, 86, -15, -17, 86, 7, Material.DARK_OAK_PLANKS);
        for (int x : new int[]{-16, 16}) tower(x, 19, 4, 93, Material.CHISELED_STONE_BRICKS, Material.COPPER_BLOCK);
    }

    private void oilRig() {
        for (int x : new int[]{-22, 22}) {
            for (int z : new int[]{-20, 15}) {
                box(x - 2, 81, z - 2, x + 2, 94, z + 2, Material.DEEPSLATE_TILES);
                box(x - 3, 88, z - 3, x + 3, 88, z + 3, Material.YELLOW_CONCRETE);
            }
        }
        box(-24, 94, -22, 24, 94, 17, Material.WAXED_CUT_COPPER);
        stairs(-9, -4, 29, -1, 14, 81, Material.DEEPSLATE_BRICKS);
        shed(-10, -6, 10, 11, Material.WHITE_CONCRETE, Material.GRAY_CONCRETE, Material.BLUE_STAINED_GLASS, 94);
        tower(14, -7, 6, 112, Material.IRON_BLOCK, Material.YELLOW_CONCRETE);
        box(7, 95, 4, 21, 95, 15, Material.YELLOW_CONCRETE);
        box(11, 95, 7, 17, 95, 12, Material.BLACK_CONCRETE);
    }

    private void reservoir() {
        for (int y = 81; y <= 97; y++) {
            int thickness = Math.max(2, 7 - (y - 81) / 3);
            box(-28, y, -thickness, 28, y, thickness, Material.POLISHED_ANDESITE);
        }
        for (int x : new int[]{-19, -9, 9, 19}) {
            box(x - 2, 81, -8, x + 2, 84, 8, Material.AIR);
            box(x - 2, 85, 5, x + 2, 89, 5, Material.CHISELED_COPPER);
        }
        stairs(23, 27, 23, -1, 17, 81, Material.STONE_BRICKS);
        shed(-16, -18, 8, 7, Material.CALCITE, Material.DARK_PRISMARINE, Material.CYAN_STAINED_GLASS);
        box(-28, 98, -2, 28, 98, 2, Material.DEEPSLATE_TILES);
        for (int x = -26; x <= 26; x += 4) put(x, 99, 2, Material.SEA_LANTERN);
    }

    private void airfield() {
        box(-8, 81, -30, 8, 81, 27, Material.GRAY_CONCRETE);
        for (int z = -26; z <= 24; z += 6) box(-1, 81, z, 1, 81, z + 2, Material.WHITE_CONCRETE);
        for (int x : new int[]{-10, 10}) {
            for (int z = -28; z <= 27; z += 8) put(x, 82, z, Material.SEA_LANTERN);
        }
        shed(-21, -12, 9, 12, Material.LIGHT_GRAY_CONCRETE, Material.WAXED_EXPOSED_COPPER, Material.GRAY_STAINED_GLASS);
        tower(21, -13, 6, 103, Material.SMOOTH_QUARTZ, Material.BLUE_STAINED_GLASS);
        box(14, 104, -20, 28, 104, -6, Material.WAXED_OXIDIZED_COPPER);
        shed(20, 13, 7, 8, Material.WHITE_CONCRETE, Material.BLUE_CONCRETE, Material.LIGHT_BLUE_STAINED_GLASS);
        stairs(13, 16, 4, -1, 7, 81, Material.IRON_BLOCK);
        box(11, 87, -10, 20, 87, -2, Material.IRON_BLOCK);
    }

    private void monastery() {
        for (int side : new int[]{-1, 1}) {
            for (int step = 0; step < 7; step++) {
                int x = 18 * side;
                box(x - 11 + step, 81 + step, -23 + step, x + 11 - step, 81 + step, 10 - step,
                    step % 2 == 0 ? Material.COBBLED_DEEPSLATE : Material.ANDESITE);
            }
            raisedHouse(18 * side, -11, 7, 7, Material.CRIMSON_PLANKS, Material.DARK_PRISMARINE);
            tower(23 * side, 14, 4, 98, Material.BRICKS, Material.DARK_PRISMARINE);
        }
        box(-22, 88, -1, 22, 88, 3, Material.MANGROVE_PLANKS);
        stairs(-5, 5, 17, -1, 8, 81, Material.MANGROVE_PLANKS);
        box(-5, 88, 3, 5, 88, 10, Material.MANGROVE_PLANKS);
        for (int x : new int[]{-16, 16}) box(x, 81, 0, x, 88, 0, Material.DEEPSLATE_BRICKS);
    }

    private void lighthouse() {
        for (int y = 81; y <= 99; y++) {
            int width = 18 - (y - 81) / 2;
            box(-width, y, -11 - width, width, y, -11 + width, y % 4 == 0 ? Material.ANDESITE : Material.TUFF);
        }
        tower(0, -11, 5, 112, Material.CALCITE, Material.RED_TERRACOTTA);
        outline(-3, -14, 3, -8, 113, Material.YELLOW_STAINED_GLASS);
        box(-3, 114, -14, 3, 114, -8, Material.YELLOW_STAINED_GLASS);
        put(0, 113, -11, Material.SEA_LANTERN);
        box(-5, 115, -16, 5, 115, -6, Material.DEEPSLATE_TILES);
        stairs(-2, 2, 26, -1, 18, 81, Material.TUFF_BRICKS);
        shed(-21, 10, 7, 7, Material.WARPED_PLANKS, Material.WAXED_COPPER_BLOCK, Material.LIGHT_BLUE_STAINED_GLASS);
        shed(21, 10, 7, 7, Material.DARK_OAK_PLANKS, Material.WAXED_WEATHERED_COPPER, Material.BLUE_STAINED_GLASS);
    }

    private void ship() {
        for (int z = -30; z <= 24; z++) {
            int width = Math.min(12, 3 + (z + 30) / 3);
            width = Math.min(width, 4 + (24 - z) / 2);
            box(-width, 81, z, width, 85, z, Material.DARK_OAK_PLANKS);
            box(-width, 86, z, width, 86, z, Material.SPRUCE_PLANKS);
            put(-width, 87, z, Material.DARK_OAK_FENCE);
            put(width, 87, z, Material.DARK_OAK_FENCE);
        }
        stairs(-2, 2, 29, -1, 6, 81, Material.SPRUCE_PLANKS);
        for (int z : new int[]{-16, 4}) {
            box(0, 87, z, 0, 107, z, Material.STRIPPED_DARK_OAK_LOG);
            box(-8, 105, z, 8, 105, z, Material.DARK_OAK_PLANKS);
            for (int y = 96; y <= 104; y++) {
                int width = 7 - (104 - y) / 2;
                box(-width, y, z + (y % 3 == 0 ? 1 : 0), width, y, z + (y % 3 == 0 ? 1 : 0), Material.WHITE_WOOL);
            }
        }
        shed(0, 15, 5, 5, Material.SPRUCE_PLANKS, Material.DARK_PRISMARINE, Material.YELLOW_STAINED_GLASS, 86);
        for (int side : new int[]{-1, 1}) {
            box(side * 23 - 4, 81, -20, side * 23 + 4, 83, 20, Material.SPRUCE_PLANKS);
            box(side * 15 - 5, 85, 3, side * 15 + 5, 85, 7, Material.OAK_PLANKS);
        }
    }

    private void citadel() {
        for (int side : new int[]{-1, 1}) {
            for (int z : new int[]{-17, 10}) {
                tower(side * 18, z, 6, 103, Material.SMOOTH_QUARTZ, Material.PURPLE_STAINED_GLASS);
                box(side * 18 - 3, 104, z - 3, side * 18 + 3, 106, z + 3, Material.AMETHYST_BLOCK);
            }
        }
        for (int z : new int[]{-17, 10}) {
            box(-18, 94, z - 3, 18, 94, z + 3, Material.MAGENTA_STAINED_GLASS);
            box(-15, 98, z - 3, 15, 98, z + 3, Material.PURPLE_STAINED_GLASS);
        }
        box(-5, 94, -17, 5, 94, 10, Material.SMOOTH_QUARTZ);
        stairs(-3, 3, 26, -1, 14, 81, Material.QUARTZ_BRICKS);
        box(-5, 94, 10, 5, 94, 14, Material.QUARTZ_BRICKS);
    }

    private void volcano() {
        for (int step = 0; step < 18; step++) {
            int width = 24 - step;
            int y = 81 + step;
            Material stone = step % 4 == 2 ? Material.BASALT : Material.POLISHED_BLACKSTONE_BRICKS;
            outline(-width, -5 - width, width, -5 + width, y, stone);
            outline(-width + 1, -4 - width, width - 1, -6 + width, y, stone);
        }
        box(-6, 97, -11, 6, 97, 1, Material.MAGMA_BLOCK);
        for (int y = 82; y <= 96; y++) put((y % 3) - 1, y, 11 - (y - 82), Material.ORANGE_CONCRETE);
        stairs(5, 8, 25, -1, 17, 81, Material.BLACKSTONE);
        shed(21, 19, 7, 5, Material.RED_NETHER_BRICKS, Material.WAXED_COPPER_BLOCK, Material.ORANGE_STAINED_GLASS);
        shed(-19, 20, 8, 5, Material.BASALT, Material.RED_NETHER_BRICKS, Material.YELLOW_STAINED_GLASS);
    }

    private void skybridge() {
        for (int z = -25; z <= 23; z += 8) {
            for (int x : new int[]{-13, 13}) {
                box(x - 2, 81, z - 1, x + 2, 92, z + 1, Material.STONE_BRICKS);
                box(x - 3, 89, z - 2, x + 3, 90, z + 2, Material.CHISELED_STONE_BRICKS);
            }
            box(-13, 92, z - 1, 13, 92, z + 1, Material.STONE_BRICKS);
        }
        box(-16, 93, -27, -10, 93, 25, Material.BIRCH_PLANKS);
        box(10, 93, -27, 16, 93, 25, Material.BIRCH_PLANKS);
        stairs(-15, -11, 29, -1, 13, 81, Material.ANDESITE);
        stairs(11, 15, 29, -1, 13, 81, Material.ANDESITE);
        for (int z : new int[]{-19, 2, 20}) {
            box(-13, 93, z - 3, 13, 93, z + 3, Material.BIRCH_PLANKS);
            box(-13, 98, z - 3, 13, 98, z + 3, Material.WAXED_OXIDIZED_COPPER);
        }
    }

    private record Trim(Material base, Material trim, Material lamp, Material prop) {}

    private void detail(MapKind map) {
        Trim trim = switch (map) {
            case PYRAMID, QUARRY, VOLCANO -> new Trim(Material.CUT_RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE,
                Material.OCHRE_FROGLIGHT, Material.DECORATED_POT);
            case HARBOR, SHIP, VILLAGE -> new Trim(Material.SPRUCE_PLANKS, Material.WAXED_EXPOSED_COPPER,
                Material.SHROOMLIGHT, Material.BARREL);
            case OBSERVATORY, CITADEL, LIGHTHOUSE -> new Trim(Material.CALCITE, Material.QUARTZ_PILLAR,
                Material.SEA_LANTERN, Material.BLUE_STAINED_GLASS);
            case GREENHOUSE, MONASTERY -> new Trim(Material.DARK_PRISMARINE, Material.POLISHED_TUFF,
                Material.VERDANT_FROGLIGHT, Material.MOSS_BLOCK);
            case FORT, LIBRARY, SKYBRIDGE -> new Trim(Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS,
                Material.SHROOMLIGHT, Material.DARK_OAK_PLANKS);
            case MARKET -> new Trim(Material.SMOOTH_SANDSTONE, Material.ORANGE_TERRACOTTA,
                Material.PEARLESCENT_FROGLIGHT, Material.GLASS);
            default -> new Trim(Material.POLISHED_DEEPSLATE, Material.WAXED_WEATHERED_COPPER,
                Material.SEA_LANTERN, Material.YELLOW_CONCRETE);
        };
        for (int side : new int[]{-1, 1}) {
            int x = 25 * side;
            for (int z : new int[]{-28, -16, -4, 8, 20}) {
                int position = x + (z % 8 == 0 ? side * 4 : 0);
                if (canDecorate(position, z)) {
                    put(position, 81, z, trim.base);
                    put(position, 82, z, trim.trim);
                    put(position, 83, z, trim.lamp);
                }
                if (canDecorate(position - side * 2, z + 2)) {
                    put(position - side * 2, 81, z + 2, trim.prop);
                }
            }
        }
        switch (map) {
            case PYRAMID -> {
                for (int x : new int[]{-18, 18}) {
                    box(x - 2, 81, 26, x + 2, 81, 31, Material.CHISELED_SANDSTONE);
                    box(x - 1, 82, 27, x + 1, 84, 29, Material.CUT_SANDSTONE);
                    box(x - 1, 85, 28, x + 1, 85, 28, Material.GOLD_BLOCK);
                }
            }
            case STATION -> {
                for (int z = -18; z <= 18; z += 9) {
                    box(-6, 82, z, -4, 82, z + 2, Material.POLISHED_ANDESITE);
                    put(-4, 83, z, Material.YELLOW_CONCRETE);
                }
                for (int x : new int[]{-7, 7}) box(x, 92, -17, x, 92, -4, Material.OCHRE_FROGLIGHT);
            }
            case SHIP -> {
                for (int z : new int[]{-21, -12, -4, 5}) {
                    for (int x : new int[]{-7, 7}) {
                        box(x - 1, 87, z, x + 1, 87, z + 1, Material.IRON_BLOCK);
                        put(x, 88, z, Material.DISPENSER);
                    }
                }
            }
            case OBSERVATORY -> {
                for (int side : new int[]{-1, 1}) {
                    for (int y = 101; y <= 108; y++) {
                        int x = side * (y - 98);
                        box(x, y, -17 - (y - 101), x, y, -14 - (y - 101), Material.WHITE_CONCRETE);
                    }
                }
                box(-10, 109, -25, 10, 109, -23, Material.LIGHT_BLUE_STAINED_GLASS);
            }
            case AIRFIELD -> {
                box(-2, 82, -4, 2, 83, 15, Material.IRON_BLOCK);
                box(-11, 83, 5, 11, 83, 9, Material.WHITE_CONCRETE);
                box(-4, 84, 6, 4, 84, 9, Material.LIGHT_BLUE_STAINED_GLASS);
                box(0, 84, -5, 0, 88, -3, Material.RED_CONCRETE);
                put(0, 83, 16, Material.SEA_LANTERN);
            }
            case MARKET -> {
                for (int z : new int[]{-18, -5, 7, 18}) {
                    box(-9, 89, z, 9, 89, z, Material.DARK_OAK_FENCE);
                    for (int x = -8; x <= 8; x += 4) put(x, 88, z, Material.SHROOMLIGHT);
                }
            }
            case LIGHTHOUSE -> {
                for (int side : new int[]{-1, 1}) {
                    box(side * 10 - 1, 81, 17, side * 10 + 1, 84, 24, Material.DARK_PRISMARINE);
                    put(side * 10, 85, 22, Material.SEA_LANTERN);
                }
            }
            case VOLCANO -> {
                for (int i = 0; i < 10; i++) {
                    int x = -11 + i / 3;
                    put(x, 81 + i, 19 - i, i % 2 == 0 ? Material.MAGMA_BLOCK : Material.RED_NETHER_BRICKS);
                }
            }
            default -> { }
        }
    }

    private boolean canDecorate(int x, int z) {
        return world.getBlockAt(x, PLATFORM + 1, z).getType().isAir()
            && world.getBlockAt(x, PLATFORM + 2, z).getType().isAir()
            && world.getBlockAt(x, PLATFORM + 3, z).getType().isAir();
    }

    private void raisedHouse(int x, int z, int width, int depth, Material wall, Material roof) {
        box(x - width, 85, z - depth, x + width, 85, z + depth, Material.SPRUCE_PLANKS);
        for (int sideX : new int[]{-1, 1}) {
            for (int sideZ : new int[]{-1, 1}) {
                int px = x + sideX * (width - 1);
                int pz = z + sideZ * (depth - 1);
                box(px, 81, pz, px, 85, pz, Material.SPRUCE_LOG);
            }
        }
        shed(x, z, width, depth, wall, roof, Material.ORANGE_STAINED_GLASS, 85);
    }

    private void tower(int x, int z, int width, int top, Material wall, Material cap) {
        for (int y = 81; y < top; y++) {
            outline(x - width, z - width, x + width, z + width, y, y % 5 == 0 ? cap : wall);
        }
        box(x - width - 1, top, z - width - 1, x + width + 1, top, z + width + 1, cap);
        box(x - 1, 81, z + width, x + 1, 84, z + width, Material.AIR);
        for (int height = 0; height < top - 81; height += 6) {
            int edge = x + ((height / 6 % 2 == 0) ? -width + 1 : width - 2);
            int steps = Math.min(6, top - 81 - height);
            int front = height / 6 % 2 == 0 ? z + width - 1 : z - width + 1;
            stairs(edge, edge + 1, front, height / 6 % 2 == 0 ? -1 : 1, steps, 81 + height, wall);
        }
    }

    private void shed(int x, int z, int width, int depth, Material wall, Material roof, Material window) {
        shed(x, z, width, depth, wall, roof, window, 81);
    }

    private void shed(int x, int z, int width, int depth, Material wall, Material roof, Material window, int floor) {
        for (int y = floor + 1; y <= floor + 6; y++) {
            outline(x - width, z - depth, x + width, z + depth, y, wall);
        }
        box(x - 2, floor + 1, z + depth, x + 2, floor + 4, z + depth, Material.AIR);
        box(x - 2, floor + 1, z - depth, x + 2, floor + 4, z - depth, Material.AIR);
        for (int side : new int[]{-1, 1}) {
            box(x + side * width, floor + 3, z - depth + 2, x + side * width, floor + 4, z + depth - 2, window);
        }
        for (int row = 0; row < 4; row++) {
            box(x - width - 1 + row, floor + 7 + row, z - depth - 1,
                x + width + 1 - row, floor + 7 + row, z + depth + 1, roof);
        }
        for (int dx = -width + 2; dx <= width - 2; dx += 4) {
            put(x + dx, floor + 6, z + depth - 2, Material.SHROOMLIGHT);
            put(x + dx, floor + 6, z - depth + 2, Material.SHROOMLIGHT);
        }
    }

    private void stairs(int x1, int x2, int firstZ, int direction, int height, int firstY, Material material) {
        for (int i = 0; i < height; i++) {
            int z = firstZ + direction * i;
            box(x1, firstY, z, x2, firstY + i, z, material);
        }
    }

    private void outline(int x1, int z1, int x2, int z2, int y, Material material) {
        box(x1, y, z1, x2, y, z1, material);
        box(x1, y, z2, x2, y, z2, material);
        box(x1, y, z1, x1, y, z2, material);
        box(x2, y, z1, x2, y, z2, material);
    }

    private void box(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) put(x, y, z, material);
            }
        }
    }

    private void put(int x, int y, int z, Material material) {
        Position position = new Position(x, y, z);
        if (trackStructure) {
            if (material.isAir()) structures.remove(position);
            else structures.add(position);
        }
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) block.setType(material, false);
    }

    static final class Ocean extends ChunkGenerator {
        @Override
        public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunk) {
            chunk.setRegion(0, 48, 0, 16, 49, 16, Material.BEDROCK);
            chunk.setRegion(0, 49, 0, 16, 55, 16, Material.STONE);
            chunk.setRegion(0, 55, 0, 16, 57, 16, Material.GRAVEL);
            chunk.setRegion(0, 57, 0, 16, SEA + 1, 16, Material.WATER);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int wx = (chunkX << 4) + x;
                    int wz = (chunkZ << 4) + z;
                    double hill = island(wx, wz, 100, 46, 37, 35) + island(wx, wz, -100, -5, 43, 42)
                        + island(wx, wz, 22, 118, 34, 28) + island(wx, wz, -71, -120, 27, 29);
                    if (hill < 1) continue;
                    int top = 56 + (int) (hill + 1.5 * Math.sin(wx * 0.23) * Math.sin(wz * 0.27));
                    if (top < 58) continue;
                    chunk.setRegion(x, 57, z, x + 1, top - 1, z + 1, Material.STONE);
                    chunk.setBlock(x, top - 1, z, top > SEA + 3 ? Material.DIRT : Material.SANDSTONE);
                    chunk.setBlock(x, top, z, top > SEA + 3 ? Material.GRASS_BLOCK : Material.SAND);
                }
            }
        }

        private static double island(int x, int z, int cx, int cz, int width, int height) {
            double distance = Math.hypot(x - cx, z - cz) / width;
            return height * Math.max(0, 1 - distance * distance);
        }

        @Override
        public BiomeProvider getDefaultBiomeProvider(WorldInfo info) {
            return new BiomeProvider() {
                @Override
                public Biome getBiome(WorldInfo world, int x, int y, int z) {
                    return Biome.OCEAN;
                }

                @Override
                public List<Biome> getBiomes(WorldInfo world) {
                    return List.of(Biome.OCEAN);
                }
            };
        }
    }
}

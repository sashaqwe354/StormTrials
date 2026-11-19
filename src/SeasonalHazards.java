package stormtrials;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

final class SeasonalHazards {
    enum Pattern { BARRAGE, SWEEP, FIELD, VORTEX }

    enum Event {
        MAGMA_SHOWER("Магмовый ливень", "Отходи от огненных кругов, вспышки поджигают.", Pattern.BARRAGE, Particle.FLAME, Material.MAGMA_BLOCK, 4, 4, 8, 4, 65, PotionEffectType.WEAKNESS),
        ICE_COMETS("Ледяные кометы", "Кометы скользят по диагонали и замедляют при попадании.", Pattern.BARRAGE, Particle.SNOWFLAKE, Material.PACKED_ICE, 5, 5, 6, 3, 32, PotionEffectType.SLOWNESS),
        FALLING_SATELLITES("Орбитальный обвал", "Редкие крупные обломки, широкая зона удара.", Pattern.BARRAGE, Particle.ELECTRIC_SPARK, Material.IRON_BLOCK, 9, 7, 12, 5, 115, null),
        ROCKFALL("Камнепад", "Прижмись к глухой стене и следи за падающими камнями.", Pattern.BARRAGE, Particle.CAMPFIRE_COSY_SMOKE, Material.ANDESITE, 3, 3, 5, 3, 42, null),
        LIGHTNING_ORBS("Шаровые молнии", "Огни преследуют нижние площадки и наносят повторные удары.", Pattern.BARRAGE, Particle.END_ROD, Material.COPPER_BULB, 5, 4, 7, 4, 28, PotionEffectType.GLOWING),
        SHARD_STORM("Стеклянный шквал", "Мелкие осколки летят сериями вдоль настилов.", Pattern.BARRAGE, Particle.CRIT, Material.LIGHT_BLUE_STAINED_GLASS, 3, 3, 4, 2, 18, null),
        ASH_BOMBS("Пепельные бомбы", "После удара пепел ненадолго закрывает обзор.", Pattern.BARRAGE, Particle.ASH, Material.POLISHED_BLACKSTONE, 6, 5, 7, 4, 48, PotionEffectType.BLINDNESS),
        SALT_BARRAGE("Соляная картечь", "Белые снаряды сбивают ритм бега на открытой местности.", Pattern.BARRAGE, Particle.WHITE_ASH, Material.SMOOTH_QUARTZ, 4, 3, 5, 3, 24, PotionEffectType.SLOWNESS),

        TIDAL_SURGE("Приливный вал", "Синяя полоса движется поперёк карты. Поднимайся выше.", Pattern.SWEEP, Particle.SPLASH, Material.PRISMARINE, 11, 5, 7, 4, 29, null),
        SAND_WALL("Песчаная стена", "Уходи в сторону от диагональной стены пыли.", Pattern.SWEEP, Particle.FALLING_DUST, Material.SANDSTONE, 10, 4, 4, 4, 23, PotionEffectType.BLINDNESS),
        PRESSURE_FRONT("Фронт давления", "Белые полосы отбрасывают игроков от основания.", Pattern.SWEEP, Particle.CLOUD, Material.CALCITE, 9, 5, 4, 4, 20, null),
        FROST_FRONT("Морозный фронт", "Ледяная полоса проходит с востока на запад и замедляет.", Pattern.SWEEP, Particle.SNOWFLAKE, Material.BLUE_ICE, 12, 5, 5, 4, 24, PotionEffectType.SLOWNESS),
        HEAT_WAVE("Волна жара", "Линия жара идёт поперёк карты. Укройся за стеной или под крышей.", Pattern.SWEEP, Particle.FLAME, Material.RED_SANDSTONE, 13, 4, 6, 4, 20, PotionEffectType.WEAKNESS),
        SEISMIC_FAULT("Бегущий разлом", "Красные искры показывают путь разлома по нижним этажам.", Pattern.SWEEP, Particle.CRIMSON_SPORE, Material.RED_TERRACOTTA, 12, 4, 7, 3, 44, null),

        TOXIC_FOG("Ядовитый туман", "Не задерживайся в зелёных облаках у земли.", Pattern.FIELD, Particle.FALLING_SPORE_BLOSSOM, Material.MOSS_BLOCK, 8, 6, 3, 5, 0, PotionEffectType.POISON),
        GEYSERS("Гейзеры", "Выходи из кругов пара до выброса вверх.", Pattern.FIELD, Particle.CLOUD, Material.CALCITE, 7, 4, 5, 3, 25, null),
        SINKHOLES("Просадка грунта", "Серые круги ослабляют низ построек и замедляют.", Pattern.FIELD, Particle.CAMPFIRE_COSY_SMOKE, Material.TUFF, 8, 5, 4, 4, 42, PotionEffectType.SLOWNESS),
        ELECTRICAL_GRID("Электрическая сеть", "Зоны искр отбивают урон импульсами.", Pattern.FIELD, Particle.ELECTRIC_SPARK, Material.COPPER_BLOCK, 9, 6, 6, 4, 12, PotionEffectType.GLOWING),
        STATIC_STORM("Статический туман", "Фиолетовое поле сбивает ориентацию, но редко ломает укрытия.", Pattern.FIELD, Particle.REVERSE_PORTAL, Material.CRYING_OBSIDIAN, 8, 5, 3, 5, 0, PotionEffectType.DARKNESS),
        SPORE_BLOOM("Споровая вспышка", "Серые споры замедляют движение в плотном облаке.", Pattern.FIELD, Particle.SPORE_BLOSSOM_AIR, Material.MUD, 7, 5, 4, 4, 8, PotionEffectType.SLOWNESS),

        WATERSPOUT("Водяной смерч", "Голубая воронка вытягивает вверх и отбрасывает.", Pattern.VORTEX, Particle.SPLASH, Material.PRISMARINE, 13, 6, 5, 5, 28, null),
        DUST_DEVILS("Пыльные вихри", "Две пыльные воронки проходят встречными путями.", Pattern.VORTEX, Particle.FALLING_DUST, Material.SANDSTONE, 12, 5, 4, 4, 17, PotionEffectType.BLINDNESS),
        GRAVITY_WELL("Гравитационный вихрь", "Тёмная воронка притягивает к эпицентру.", Pattern.VORTEX, Particle.REVERSE_PORTAL, Material.OBSIDIAN, 15, 7, 6, 5, 25, PotionEffectType.SLOWNESS);

        final String name;
        final String hint;
        final Pattern pattern;
        final Particle marker;
        final Material material;
        final int interval;
        final int length;
        final int damage;
        final int radius;
        final int breakLimit;
        final PotionEffectType debuff;

        Event(String name, String hint, Pattern pattern, Particle marker, Material material,
              int interval, int length, int damage, int radius, int breakLimit, PotionEffectType debuff) {
            this.name = name;
            this.hint = hint;
            this.pattern = pattern;
            this.marker = marker;
            this.material = material;
            this.interval = interval;
            this.length = length;
            this.damage = damage;
            this.radius = radius;
            this.breakLimit = breakLimit;
            this.debuff = debuff;
        }
    }

    private record Motion(Location target, int start, int end, int sign, BlockDisplay body) {}

    private final World world;
    private final Arenas arenas;
    private final Predicate<Player> activePlayer;
    private final BiConsumer<Player, PotionEffect> applyEffect;
    private final List<Motion> motions = new ArrayList<>();
    private Event event;
    private int broken;

    SeasonalHazards(World world, Arenas arenas, Predicate<Player> activePlayer, BiConsumer<Player, PotionEffect> applyEffect) {
        this.world = world;
        this.arenas = arenas;
        this.activePlayer = activePlayer;
        this.applyEffect = applyEffect;
    }

    void begin(Event next) {
        clear();
        event = next;
    }

    void clear() {
        for (Motion motion : motions) if (motion.body != null) motion.body.remove();
        motions.clear();
        event = null;
        broken = 0;
    }

    int collectBroken() {
        int value = broken;
        broken = 0;
        return value;
    }

    void tick(int active, List<Player> players) {
        if (event == null) return;
        if (active % event.interval == 0) {
            switch (event.pattern) {
                case BARRAGE -> {
                    launch(target(players), active, 1);
                    if (event != Event.FALLING_SATELLITES) launch(structure(), active, -1);
                }
                case SWEEP -> {
                    launch(new Location(world, 0.5, Arenas.PLATFORM + 1, 0.5), active, active / event.interval % 2 == 0 ? 1 : -1);
                }
                case FIELD -> {
                    launch(target(players), active, 1);
                    launch(structure(), active, -1);
                }
                case VORTEX -> {
                    launch(new Location(world, 0.5, Arenas.PLATFORM + 1, 0.5), active, 1);
                    if (event == Event.DUST_DEVILS) launch(new Location(world, 0.5, Arenas.PLATFORM + 1, 0.5), active, -1);
                }
            }
        }
        motions.removeIf(motion -> {
            if (active < motion.end && event.pattern == Pattern.BARRAGE) return false;
            if (event.pattern == Pattern.BARRAGE) {
                if (motion.body != null) motion.body.remove();
                land(motion.target, players);
                return true;
            }
            if (active >= motion.end) return true;
            if (active < motion.start + 2) return false;
            switch (event.pattern) {
                case SWEEP -> sweep(motion, active, players);
                case FIELD -> {
                    if (active % 2 == 0) land(motion.target, players);
                }
                case VORTEX -> land(vortex(motion, active), players);
                default -> { }
            }
            return false;
        });
    }

    void animate(double active) {
        if (event == null || active < 0) return;
        for (Motion motion : motions) {
            switch (event.pattern) {
                case BARRAGE -> {
                    double left = Math.max(0, (motion.end - active) / (motion.end - motion.start));
                    Location path = motion.target.clone().add(motion.sign * left * 12, left * left * 25 + 0.5, left * 5);
                    motion.body.teleport(path);
                    particle(path, 9, 0.45, 0.45, 0.45);
                    ring(motion.target, event.radius, 0.35, active * 2);
                }
                case SWEEP -> {
                    double along = progress(motion, active) * 62 - 31;
                    for (int i = -30; i <= 30; i += 3) {
                        Location point = sweepPoint(motion, along, i);
                        particle(point.clone().add(0, 0.4 + 2 * Math.abs(Math.sin(active + i)), 0), 5, 0.4, 0.75, 0.4);
                    }
                }
                case FIELD -> {
                    ring(motion.target, event.radius, 0.3, active * 1.6);
                    particle(motion.target.clone().add(0, 1.3, 0), 18, event.radius * 0.48, 1.2, event.radius * 0.48);
                }
                case VORTEX -> {
                    Location center = vortex(motion, active);
                    for (int layer = 0; layer < 13; layer++) {
                        double angle = active * 5 + layer * 0.77;
                        double radius = 0.8 + 0.24 * layer;
                        particle(center.clone().add(Math.cos(angle) * radius, layer * 0.83, Math.sin(angle) * radius), 5, 0.35, 0.4, 0.35);
                    }
                }
            }
        }
    }

    private void launch(Location location, int active, int sign) {
        BlockDisplay body = null;
        if (event.pattern == Pattern.BARRAGE) {
            Event current = event;
            body = world.spawn(location.clone().add(sign * 12, 25, 5), BlockDisplay.class, display -> {
                display.setBlock(current.material.createBlockData());
                display.setPersistent(false);
                display.setTeleportDuration(4);
            });
        }
        int duration = event.length + (event.pattern == Pattern.SWEEP ? 2 : 0);
        motions.add(new Motion(location, active, active + duration, sign, body));
    }

    private Location target(List<Player> players) {
        if (players.isEmpty()) return structure();
        Location player = players.get(ThreadLocalRandom.current().nextInt(players.size())).getLocation();
        int top = world.getHighestBlockYAt(player.getBlockX(), player.getBlockZ());
        return new Location(world, player.getX(), Math.max(Arenas.PLATFORM + 1, top + 0.7), player.getZ());
    }

    private Location structure() {
        Location contact = arenas.findExposedStructure(ThreadLocalRandom.current(), Arenas.PLATFORM + 1, 115);
        return contact == null ? new Location(world, 0.5, Arenas.PLATFORM + 1, 0.5) : contact;
    }

    private void land(Location hit, List<Player> players) {
        if (event.breakLimit > 0) broken += arenas.damageSphere(hit, event.radius, event.breakLimit, Arenas.Damage.BLAST);
        particle(hit.clone().add(0, 0.6, 0), 34, event.radius * 0.42, 0.6, event.radius * 0.42);
        Sound sound = switch (event.pattern) {
            case BARRAGE -> Sound.ENTITY_GENERIC_EXPLODE;
            case SWEEP -> Sound.ENTITY_BREEZE_WIND_BURST;
            case FIELD -> Sound.BLOCK_FIRE_EXTINGUISH;
            case VORTEX -> Sound.ENTITY_BREEZE_IDLE_AIR;
        };
        world.playSound(hit, sound, 0.65f, 0.8f);
        for (Player player : players) {
            if (!activePlayer.test(player)) continue;
            Location loc = player.getLocation();
            if (loc.distanceSquared(hit) > event.radius * event.radius) continue;
            if (event == Event.HEAT_WAVE && world.getHighestBlockYAt(loc) > loc.getBlockY() + 1) continue;
            player.damage(event.damage);
            if (!activePlayer.test(player)) continue;
            if (event.debuff != null) applyEffect.accept(player, new PotionEffect(event.debuff, 60, 0));
            if (event == Event.MAGMA_SHOWER) player.setFireTicks(Math.max(player.getFireTicks(), 45));
            if (event == Event.GEYSERS || event == Event.PRESSURE_FRONT || event.pattern == Pattern.VORTEX) {
                Vector vector = loc.toVector().subtract(hit.toVector()).setY(0);
                if (vector.lengthSquared() < 0.01) vector.setX(1);
                player.setVelocity(vector.normalize().multiply(event == Event.GRAVITY_WELL ? -0.65 : 0.65)
                    .setY(event == Event.GEYSERS ? 0.75 : 0.24));
            }
        }
    }

    private void sweep(Motion motion, int active, List<Player> players) {
        double along = progress(motion, active) * 62 - 31;
        for (int i = -28; i <= 28; i += 7) land(sweepPoint(motion, along, i), players);
    }

    private Location sweepPoint(Motion motion, double along, int offset) {
        boolean xAxis = event == Event.FROST_FRONT || event == Event.PRESSURE_FRONT;
        if (event == Event.SAND_WALL || event == Event.SEISMIC_FAULT) along += offset * 0.18;
        double position = Math.max(-32, Math.min(32, along)) * motion.sign;
        return new Location(world, xAxis ? position : offset, Arenas.PLATFORM + 1, xAxis ? offset : position);
    }

    private Location vortex(Motion motion, double active) {
        double time = active - motion.start;
        double phase = time * 0.54 * motion.sign + event.ordinal() * 0.41;
        return motion.target.clone().add(Math.sin(phase) * 28, event == Event.WATERSPOUT ? 1.3 : 0.4,
            Math.cos(phase * 1.22) * 24);
    }

    private double progress(Motion motion, double clock) {
        int start = motion.start + 2;
        return Math.max(0, Math.min(1, (clock - start) / (motion.end - start)));
    }

    private void ring(Location center, double radius, double y, double spin) {
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6 + spin;
            particle(center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius), 3, 0.14, 0.19, 0.14);
        }
    }

    private void particle(Location center, int count, double x, double y, double z) {
        if (event.marker == Particle.FALLING_DUST) {
            world.spawnParticle(event.marker, center, count, x, y, z, 0.03, event.material.createBlockData());
        } else {
            world.spawnParticle(event.marker, center, count, x, y, z, 0.03);
        }
    }
}

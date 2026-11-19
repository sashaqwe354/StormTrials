package stormtrials;

import java.util.ArrayList;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class StormTrials extends JavaPlugin implements Listener {
    private static final String WORLD = "storm_islands";
    private static final int COUNTDOWN = 40;
    private static final int AFK_TICKS = 120 * 20;
    private static final int WARNING = 6;
    private static final int DURATION = 60;

    private enum Disaster {
        FLOOD("Наводнение", "Выше! Вода поднимается, нижние этажи размывает."),
        ACID("Кислотный дождь", "Ищи крышу. Кислота постепенно разъедает укрытия."),
        METEORS("Метеоритный шквал", "Уходи из кругов огня. Удары разрушают стены."),
        GALE("Шквальный ветер", "Укрывайся за стенами. Ветер сносит блоки и игроков."),
        THUNDER("Грозовой фронт", "Уходи из отмеченных искрами зон до удара молнии."),
        HAIL("Ледяной град", "Ищи крышу и уходи из голубых кругов."),
        QUAKE("Землетрясение", "Пересекай линию толчка, прежде чем земля под ней дрогнет."),
        MAGMA_SHOWER(SeasonalHazards.Event.MAGMA_SHOWER), ICE_COMETS(SeasonalHazards.Event.ICE_COMETS),
        FALLING_SATELLITES(SeasonalHazards.Event.FALLING_SATELLITES), ROCKFALL(SeasonalHazards.Event.ROCKFALL),
        LIGHTNING_ORBS(SeasonalHazards.Event.LIGHTNING_ORBS), SHARD_STORM(SeasonalHazards.Event.SHARD_STORM),
        ASH_BOMBS(SeasonalHazards.Event.ASH_BOMBS), SALT_BARRAGE(SeasonalHazards.Event.SALT_BARRAGE),
        TIDAL_SURGE(SeasonalHazards.Event.TIDAL_SURGE), SAND_WALL(SeasonalHazards.Event.SAND_WALL),
        PRESSURE_FRONT(SeasonalHazards.Event.PRESSURE_FRONT), FROST_FRONT(SeasonalHazards.Event.FROST_FRONT),
        HEAT_WAVE(SeasonalHazards.Event.HEAT_WAVE), SEISMIC_FAULT(SeasonalHazards.Event.SEISMIC_FAULT),
        TOXIC_FOG(SeasonalHazards.Event.TOXIC_FOG), GEYSERS(SeasonalHazards.Event.GEYSERS),
        SINKHOLES(SeasonalHazards.Event.SINKHOLES), ELECTRICAL_GRID(SeasonalHazards.Event.ELECTRICAL_GRID),
        STATIC_STORM(SeasonalHazards.Event.STATIC_STORM), SPORE_BLOOM(SeasonalHazards.Event.SPORE_BLOOM),
        WATERSPOUT(SeasonalHazards.Event.WATERSPOUT), DUST_DEVILS(SeasonalHazards.Event.DUST_DEVILS),
        GRAVITY_WELL(SeasonalHazards.Event.GRAVITY_WELL);

        final String name;
        final String hint;
        final SeasonalHazards.Event profile;

        Disaster(String name, String hint) {
            this.name = name;
            this.hint = hint;
            profile = null;
        }

        Disaster(SeasonalHazards.Event profile) {
            this.profile = profile;
            name = profile.name;
            hint = profile.hint;
        }
    }

    private enum Boost {
        SPRINT("Попутный ветер"), SPRING("Пружинный шаг"), RECOVERY("Импульсы лечения"), PADDED("Мягкая посадка");
        final String title;
        Boost(String title) { this.title = title; }
    }

    private enum Penalty {
        DARK("Провалы видимости"), SLOW("Вязкий воздух"), FRAGILE("Повышенный урон"), EXPOSED("Светящийся след");
        final String title;
        Penalty(String title) { this.title = title; }
    }

    private enum Perk {
        MEDKIT("Аптечка", "ПКМ в раунде восстанавливает 5 сердец.", Material.GOLDEN_APPLE, 5, 11),
        SPEED("Ускорение", "Скорость на 30 секунд с начала бедствия.", Material.SUGAR, 7, 13),
        SHIELD("Бронезащита", "Сопротивление урону на 30 секунд.", Material.SHIELD, 9, 15),
        JUMP("Высокий прыжок", "Выше прыжок на 30 секунд.", Material.RABBIT_FOOT, 8, 10),
        FEATHER("Мягкое падение", "Замедленное падение на 30 секунд.", Material.FEATHER, 8, 16);

        final String title;
        final String description;
        final Material icon;
        final int cost;
        final int slot;

        Perk(String title, String description, Material icon, int cost, int slot) {
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.cost = cost;
            this.slot = slot;
        }
    }

    private record Meteor(Location point, int strike, BlockDisplay body) {}
    private record Impact(Location point, int strike, Arenas.Damage cause) {}
    private record WeatherHit(Location point, int strike, BlockDisplay hailstone) {}

    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Set<UUID> watching = new LinkedHashSet<>();
    private final Set<UUID> managed = new LinkedHashSet<>();
    private final Set<UUID> builders = new LinkedHashSet<>();
    private final Set<UUID> internalTeleport = new LinkedHashSet<>();
    private final Set<UUID> away = new LinkedHashSet<>();
    private final Map<UUID, Integer> lastActivity = new HashMap<>();
    private final Map<UUID, Inventory> shops = new HashMap<>();
    private final Map<UUID, Perk> roundPerks = new HashMap<>();
    private final List<Meteor> meteors = new ArrayList<>();
    private final List<Impact> impacts = new ArrayList<>();
    private final List<WeatherHit> weatherHits = new ArrayList<>();
    private NamespacedKey buttonKey;
    private World world;
    private Arenas maps;
    private Supplies supplies;
    private Achievements achievements;
    private SeasonalHazards seasonal;
    private Boost boost;
    private Penalty penalty;
    private int lastSecondTick;
    private Arenas.MapKind currentMap;
    private Disaster disaster;
    private int nextDisaster;
    private int countdown = COUNTDOWN;
    private int elapsed;
    private int broken;
    private Location lobby;

    @Override
    public void onEnable() {
        buttonKey = new NamespacedKey(this, "button");
        world = new WorldCreator(WORLD).generator(new Arenas.Ocean()).generateStructures(false).createWorld();
        if (world == null) throw new IllegalStateException("Could not create the StormTrials ocean");
        world.setDifficulty(Difficulty.NORMAL);
        world.setSpawnFlags(false, false);
        world.setTime(5700);
        world.setStorm(false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        world.setViewDistance(8);
        world.setSimulationDistance(4);
        lobby = getConfig().getLocation("lobby-location", Arenas.lobby(world));
        world.setSpawnLocation(lobby);
        maps = new Arenas(world);
        if (!getConfig().getBoolean("ocean-built")) {
            maps.buildSite();
            world.save();
            getConfig().set("ocean-built", true);
            saveConfig();
        } else {
            maps.clearOldMap();
        }
        try {
            currentMap = Arenas.MapKind.valueOf(getConfig().getString("current-arena", "PYRAMID"));
        } catch (IllegalArgumentException invalid) {
            currentMap = Arenas.MapKind.PYRAMID;
        }
        maps.load(currentMap);
        achievements = new Achievements(this);
        supplies = new Supplies(this, world, achievements, player -> alive.contains(player.getUniqueId()));
        seasonal = new SeasonalHazards(world, maps, player -> alive.contains(player.getUniqueId()),
            (player, effect) -> supplies.timedEffect(player, effect.getType(), effect.getDuration(), effect.getAmplifier()));
        nextDisaster = random().nextInt(Disaster.values().length);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::animateDisaster, 4L, 4L);
        getLogger().info("StormTrials tower ready: " + Arenas.MapKind.values().length
            + " rotating maps, " + Disaster.values().length + " disasters, "
            + Supplies.Gear.values().length + " recipes and achievements.");
    }

    @Override
    public void onDisable() {
        for (Meteor meteor : meteors) meteor.body().remove();
        clearWeatherHits();
        if (seasonal != null) seasonal.clear();
        if (supplies != null) supplies.endRound();
        if (world != null) world.setStorm(false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("maps")) {
            for (Arenas.MapKind map : Arenas.MapKind.values()) tell(sender, map.name);
            return true;
        }
        if (action.equals("disasters")) {
            for (Disaster available : Disaster.values()) tell(sender, available.name + " (" + available.name().toLowerCase(Locale.ROOT) + ")");
            return true;
        }
        if (action.equals("map") && sender.hasPermission("stormtrials.start")) {
            if (disaster != null || args.length != 2) {
                tell(sender, "В перерыве: /storm map <" + String.join(", ",
                    Arrays.stream(Arenas.MapKind.values()).map(Enum::name).toList()) + ">");
            } else {
                try {
                    selectMap(Arenas.MapKind.valueOf(args[1].toUpperCase(Locale.ROOT)));
                    tell(sender, "Выбрана карта: " + currentMap.name);
                } catch (IllegalArgumentException invalid) {
                    tell(sender, "Карта не найдена. /storm map — показать ключи карт.");
                }
            }
            return true;
        }
        if (action.equals("start")) {
            if (!sender.hasPermission("stormtrials.start")) tell(sender, "Только для администратора.");
            else if (disaster != null || participants().isEmpty()) tell(sender, "Нужен игрок на сервере и свободная арена.");
            else {
                if (args.length > 1) {
                    try {
                        nextDisaster = Disaster.valueOf(args[1].toUpperCase(Locale.ROOT)).ordinal();
                    } catch (IllegalArgumentException invalid) {
                        tell(sender, "Введи /storm disasters для списка ключей.");
                        return true;
                    }
                }
                countdown = 1;
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            tell(sender, "Из консоли доступны storm maps и storm start.");
            return true;
        }
        switch (action) {
            case "join" -> {
                setPaused(player, false);
                away.remove(player.getUniqueId());
                markActive(player);
                roundInfo(player);
            }
            case "lobby", "leave" -> sendToTower(player);
            case "pause" -> setPaused(player, !paused(player));
            case "afk" -> toggleAfk(player);
            case "build" -> toggleBuild(player);
            case "setlobby" -> setLobby(player);
            case "shop" -> supplies.openShop(player);
            case "perks" -> openShop(player);
            case "kit", "accessories" -> {
                if (!builders.contains(player.getUniqueId())) supplies.open(player);
            }
            case "spectate" -> spectate(player);
            case "stats" -> stats(player);
            case "achievements", "medals" -> { if (!builders.contains(player.getUniqueId())) achievements.open(player); }
            default -> tell(player, "/storm shop — магазин | /storm pause — участие | /storm afk — пауза до возвращения"
                + " | /storm spectate — наблюдать | /storm disasters — стихии");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("map") && sender.hasPermission("stormtrials.start")) {
            return Arrays.stream(Arenas.MapKind.values()).map(Enum::name)
                .filter(name -> name.startsWith(args[1].toUpperCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start") && sender.hasPermission("stormtrials.start")) {
            return Arrays.stream(Disaster.values()).map(value -> value.name().toLowerCase(Locale.ROOT))
                .filter(name -> name.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length != 1) return List.of();
        return List.of("join", "lobby", "leave", "pause", "afk", "maps", "disasters", "shop", "perks", "kit",
            "accessories", "spectate", "stats", "achievements", "start", "map", "build", "setlobby").stream()
            .filter(action -> (!action.equals("start") && !action.equals("map")) || sender.hasPermission("stormtrials.start"))
            .filter(action -> (!action.equals("build") && !action.equals("setlobby")) || sender.hasPermission("stormtrials.build"))
            .filter(action -> action.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
    }

    private void tick() {
        lastSecondTick = Bukkit.getCurrentTick();
        if (disaster == null) {
            List<Player> players = participants();
            if (players.isEmpty()) {
                countdown = COUNTDOWN;
            } else if (--countdown <= 0) {
                start(players);
            } else {
                for (Player player : players) {
                    bar(player, "Автостарт через " + countdown + " с  •  " + currentMap.name);
                    if (countdown <= 3) {
                        player.showTitle(Title.title(Component.text(countdown, countdown == 1 ? NamedTextColor.RED : NamedTextColor.GOLD),
                            Component.text(currentMap.name), Title.Times.times(Duration.ZERO, Duration.ofMillis(750), Duration.ofMillis(150))));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.55f, 1.05f + (3 - countdown) * 0.2f);
                    }
                }
            }
            return;
        }
        if (alive.isEmpty() || elapsed >= WARNING + DURATION) {
            finish();
            return;
        }
        for (UUID id : watching) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) bar(player, "Наблюдение • " + disaster.name + " • В живых: " + alive.size() + " • /storm lobby");
        }
        for (UUID id : List.copyOf(alive)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;
            if (water(player.getLocation())) {
                knockOut(player, "Ты коснулся воды.");
            } else {
                int remaining = elapsed < WARNING ? WARNING - elapsed : WARNING + DURATION - elapsed;
                bar(player, disaster.name + "  •  " + remaining + " с  •  В живых: " + alive.size());
            }
        }
        if (elapsed == WARNING) {
            for (UUID id : alive) {
                Player player = Bukkit.getPlayer(id);
                Perk perk = roundPerks.get(id);
                if (player != null && perk != null && perk != Perk.MEDKIT) effect(player, potion(perk));
            }
        }
        if (!alive.isEmpty()) {
            if (elapsed >= WARNING) {
                applyModifiers(elapsed - WARNING);
                runDisaster(elapsed - WARNING);
            }
            if (!alive.isEmpty()) supplies.tick(elapsed);
        }
        elapsed++;
    }

    private List<Player> participants() {
        return Bukkit.getOnlinePlayers().stream()
            .filter(player -> managed.contains(player.getUniqueId()) && !builders.contains(player.getUniqueId())
                && !paused(player) && !isAfk(player))
            .map(player -> (Player) player).toList();
    }

    private void start(List<Player> players) {
        disaster = Disaster.values()[nextDisaster++ % Disaster.values().length];
        seasonal.begin(disaster.profile);
        boost = Boost.values()[random().nextInt(Boost.values().length)];
        penalty = Penalty.values()[random().nextInt(Penalty.values().length)];
        countdown = COUNTDOWN;
        elapsed = 0;
        broken = 0;
        int index = 0;
        for (Player player : players) {
            UUID id = player.getUniqueId();
            player.closeInventory();
            watching.remove(id);
            equip(player);
            if (!teleport(player, maps.spawn(currentMap, index++))) {
                sendToTower(player);
                continue;
            }
            alive.add(id);
            supplies.enter(player);
            player.getInventory().setItem(0, button("kit", Material.CRAFTING_TABLE, "Полевой верстак",
                "ПКМ: крафты и применение.", "Материалы лежат в аирдропах."));
            increment(player, "played", 1);
            achievements.play(player, currentMap.name());
            Perk perk = reserved(player);
            if (perk != null) {
                roundPerks.put(id, perk);
                getConfig().set(score(player, "perk"), null);
                if (perk == Perk.MEDKIT) player.getInventory().setItem(4, button("heal", perk.icon, perk.title, perk.description));
            }
            player.getInventory().setItem(8, button("lobby", Material.NETHER_STAR, "Вернуться в башню", "ПКМ завершает участие в раунде."));
            player.showTitle(Title.title(Component.text(currentMap.name, NamedTextColor.GOLD),
                Component.text(disaster.name + " через " + WARNING + " с", NamedTextColor.WHITE)));
            tell(player, disaster.hint);
            tell(player, "Бафф: " + boost.title + " • Дебафф: " + penalty.title + ".");
        }
        world.setStorm(disaster == Disaster.ACID || disaster == Disaster.THUNDER);
        world.setThundering(false);
        saveConfig();
        getLogger().info("Round started: " + currentMap.name + " / " + disaster.name + " / players=" + alive.size());
    }

    private void finish() {
        int survivors = alive.size();
        for (UUID id : List.copyOf(alive)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                increment(player, "wins", 1);
                increment(player, "coins", 7);
                achievements.win(player, disaster.name());
                sendToTower(player);
                player.showTitle(Title.title(Component.text("Ты выжил!", NamedTextColor.GREEN),
                    Component.text("+7 монет", NamedTextColor.GOLD)));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                celebrate(player);
            }
        }
        getLogger().info("Round ended: survivors=" + survivors + ", destroyed map blocks=" + broken);
        saveConfig();
        world.setStorm(false);
        for (Meteor meteor : meteors) meteor.body().remove();
        meteors.clear();
        impacts.clear();
        clearWeatherHits();
        seasonal.clear();
        supplies.endRound();
        disaster = null;
        Arenas.MapKind[] rotation = Arenas.MapKind.values();
        selectMap(rotation[(currentMap.ordinal() + 1) % rotation.length]);
    }

    private void applyModifiers(int active) {
        for (UUID id : alive) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) continue;
            if (active == 0 && boost == Boost.SPRINT) supplies.timedEffect(player, PotionEffectType.SPEED, 240, 0);
            if (active == 0 && boost == Boost.SPRING) supplies.timedEffect(player, PotionEffectType.JUMP_BOOST, 300, 0);
            if (active > 0 && active % 16 == 0 && boost == Boost.RECOVERY) {
                player.setHealth(Math.min(maxHealth(player), player.getHealth() + 2));
                player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 4, 0.27, 0.3, 0.27, 0);
            }
            if (penalty == Penalty.DARK && active % 16 == 6) supplies.timedEffect(player, PotionEffectType.DARKNESS, 65, 0);
            if (penalty == Penalty.SLOW && active % 14 == 7) supplies.timedEffect(player, PotionEffectType.SLOWNESS, 80, 0);
            if (penalty == Penalty.EXPOSED && active % 14 == 5) supplies.timedEffect(player, PotionEffectType.GLOWING, 110, 0);
        }
    }

    private void animateDisaster() {
        if (disaster == null) return;
        double active = elapsed - 1 - WARNING + Math.min(0.99, (Bukkit.getCurrentTick() - lastSecondTick) / 20.0);
        if (active < 0) return;
        seasonal.animate(active);
        for (Meteor meteor : meteors) {
            double left = Math.max(0, (meteor.strike() - active) / 3.0);
            Location path = meteor.point().clone().add(left * 5, 0.5 + left * left * 19, left * 4);
            meteor.body().teleport(path);
            world.spawnParticle(Particle.FLAME, path, 12, 0.35, 0.35, 0.35, 0.04);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, path.clone().add(left * 0.9, 0.4, 0), 4, 0.3, 0.3, 0.3, 0.025);
        }
        for (WeatherHit hit : weatherHits) {
            if (hit.hailstone() == null) continue;
            double left = Math.max(0, (hit.strike() - active) / 3.0);
            Location path = hit.point().clone().add(left * 4 - 0.45, left * left * 14 + 0.5, 1.5 * left - 0.45);
            hit.hailstone().teleport(path);
            world.spawnParticle(Particle.SNOWFLAKE, path, 9, 0.25, 0.35, 0.25, 0.035);
        }
    }

    private void celebrate(Player player) {
        for (int step = 0; step < 4; step++) {
            int phase = step;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!player.isOnline() || player.getWorld() != world) return;
                Location center = player.getLocation().add(0, 0.25 + phase * 0.55, 0);
                for (int i = 0; i < 12; i++) {
                    double angle = i * Math.PI / 6 + phase * 0.3;
                    player.spawnParticle(Particle.FIREWORK, center.clone().add(Math.cos(angle) * 1.6, 0, Math.sin(angle) * 1.6),
                        2, 0.05, 0.08, 0.05, 0.035);
                }
            }, step * 4L + 1);
        }
    }

    private void selectMap(Arenas.MapKind map) {
        currentMap = map;
        maps.load(currentMap);
        getConfig().set("current-arena", map.name());
        saveConfig();
        getLogger().info("Arena loaded: " + map.name());
    }

    private void runDisaster(int active) {
        List<Player> players = alive.stream().map(Bukkit::getPlayer).filter(player -> player != null).toList();
        switch (disaster) {
            case FLOOD -> {
                int level = Arenas.SEA + active / 5;
                maps.raiseWater(level);
                if (active % 3 == 0 && level > Arenas.PLATFORM) {
                    for (int i = 0; i < 3; i++) {
                        Location contact = maps.findFloodContact(random());
                        if (contact != null) impacts.add(new Impact(contact, active + 2, Arenas.Damage.WATER));
                    }
                }
            }
            case ACID -> {
                if (active % 3 == 0) {
                    Location roof = maps.findExposedStructure(random(), Arenas.PLATFORM + 2, 113);
                    if (roof != null) {
                        impacts.add(new Impact(roof, active + 2, Arenas.Damage.ACID));
                    }
                }
                for (Player player : players) {
                    Location point = player.getLocation();
                    int roof = roofY(point);
                    if (roof == 0) {
                        world.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, point.clone().add(0, 5, 0), 65, 3, 2, 3, 0);
                        if (active % 2 == 0) player.damage(3 * supplies.acidFactor(player));
                    }
                }
            }
            case GALE -> {
                double x = Math.sin(active * 0.12 - 1.57) * 32;
                double z = Math.sin(active * 0.18) * 25;
                Location vortex = new Location(world, x, Arenas.PLATFORM + 1, z);
                for (int level = 0; level < 18; level++) {
                    for (int arm = 0; arm < 3; arm++) {
                        double angle = active * 1.6 + level * 0.65 + arm * Math.PI * 2 / 3;
                        double radius = 1.2 + level * 0.23;
                        world.spawnParticle(Particle.CLOUD, x + Math.cos(angle) * radius, vortex.getY() + level,
                            z + Math.sin(angle) * radius, 5, 0.24, 0.32, 0.24, 0.015);
                    }
                }
                world.playSound(vortex, Sound.ENTITY_BREEZE_IDLE_AIR, 1.35f, 0.75f);
                if (active > 1) {
                    for (int height = 2; height < 18; height += 4) {
                        broken += maps.damageSphere(vortex.clone().add(0, height, 0), 3, 12, Arenas.Damage.WIND);
                    }
                }
                for (Player player : players) {
                    Vector away = player.getLocation().toVector().subtract(vortex.toVector()).setY(0);
                    if (away.lengthSquared() > 0.01 && away.lengthSquared() < 56 && player.getY() < vortex.getY() + 21) {
                        double force = supplies.windFactor(player);
                        player.setVelocity(away.normalize().multiply(0.95 * force).setY(0.25 * force));
                    }
                }
            }
            case METEORS -> {
                if (active % 3 == 0) {
                    List<Player> exposed = players.stream().filter(player -> !supplies.obscured(player)).toList();
                    if (!exposed.isEmpty()) launchMeteor(exposed.get(random().nextInt(exposed.size())).getLocation(), active);
                    Location structure = maps.findExposedStructure(random(), Arenas.PLATFORM + 1, 113);
                    if (structure != null) launchMeteor(structure, active);
                }
                meteors.removeIf(meteor -> {
                    Location hit = meteor.point();
                    if (active < meteor.strike()) {
                        for (int i = 0; i < 18; i++) {
                            double rad = i * Math.PI / 9;
                            world.spawnParticle(Particle.FLAME, hit.getX() + Math.cos(rad) * 4,
                                hit.getY() + 0.2, hit.getZ() + Math.sin(rad) * 4, 1, 0, 0, 0, 0);
                        }
                        world.spawnParticle(Particle.FLAME, hit.clone().add(0, (meteor.strike() - active) * 5, 0),
                            25, 0.6, 1.6, 0.6, 0.03);
                        return false;
                    }
                    meteor.body().remove();
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, hit, 1);
                    world.playSound(hit, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.8f);
                    broken += maps.damageSphere(hit, 5, 150, Arenas.Damage.BLAST);
                    for (Player player : players) {
                        if (alive.contains(player.getUniqueId()) && player.getLocation().distanceSquared(hit) < 25) {
                            player.damage(8 * supplies.blastFactor(player));
                        }
                    }
                    return true;
                });
            }
            case THUNDER, HAIL, QUAKE -> weatherDisaster(active, players);
            default -> {
                seasonal.tick(active, players);
                broken += seasonal.collectBroken();
            }
        }
        impacts.removeIf(impact -> {
            Location point = impact.point();
            if (impact.cause() == Arenas.Damage.WATER) {
                world.spawnParticle(Particle.SPLASH, point, 38, 0.8, 0.4, 0.8, 0.05);
                world.spawnParticle(Particle.BUBBLE, point, 20, 0.8, 0.35, 0.8, 0.03);
            } else {
                world.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, point.clone().add(0, 2, 0), 60, 1.3, 1.5, 1.3, 0);
            }
            if (active < impact.strike()) return false;
            broken += maps.damageSphere(point, 2, 14, impact.cause());
            return true;
        });
    }

    private void weatherDisaster(int active, List<Player> players) {
        switch (disaster) {
            case THUNDER -> {
                if (active % 5 == 0) {
                    Location structure = maps.findExposedStructure(random(), Arenas.PLATFORM + 1, 115);
                    if (structure != null) weatherHits.add(new WeatherHit(structure, active + 2, null));
                    if (!players.isEmpty()) weatherHits.add(new WeatherHit(surfaceOf(players.get(random().nextInt(players.size()))), active + 2, null));
                }
            }
            case HAIL -> {
                if (active % 4 == 0 && !players.isEmpty()) {
                    hail(surfaceOf(players.get(random().nextInt(players.size()))), active);
                    for (int i = 0; i < 3; i++) {
                        Location structure = maps.findExposedStructure(random(), Arenas.PLATFORM + 1, 115);
                        if (structure != null) hail(structure, active);
                    }
                }
            }
            case QUAKE -> {
                if (active % 7 == 0) {
                    int z = random().nextInt(-24, 25);
                    int start = random().nextBoolean() ? 1 : -1;
                    for (int i = 0; i < 7; i++) {
                        Location point = new Location(world, start * (-27 + i * 9) + 0.5, Arenas.PLATFORM + 1, z + 0.5);
                        weatherHits.add(new WeatherHit(point, active + 2 + i / 2, null));
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected weather disaster: " + disaster);
        }

        weatherHits.removeIf(hit -> {
            Location point = hit.point();
            int delay = hit.strike() - active;
            if (delay > 0) {
                Particle marker = switch (disaster) {
                    case THUNDER -> Particle.ELECTRIC_SPARK;
                    case HAIL -> Particle.SNOWFLAKE;
                    default -> Particle.ASH;
                };
                for (int i = 0; i < 12; i++) {
                    double angle = i * Math.PI / 6;
                    world.spawnParticle(marker, point.clone().add(Math.cos(angle) * 2.7, 0.25, Math.sin(angle) * 2.7),
                        4, 0.15, 0.24, 0.15, 0.02);
                }
                return false;
            }
            switch (disaster) {
                case THUNDER -> {
                    world.strikeLightningEffect(point);
                    broken += maps.damageSphere(point, 4, 85, Arenas.Damage.BLAST);
                }
                case HAIL -> {
                    hit.hailstone().remove();
                    world.spawnParticle(Particle.BLOCK, point.clone().add(0, 0.4, 0), 50, 0.65, 0.6, 0.65, 0.07,
                        Material.PACKED_ICE.createBlockData());
                    world.playSound(point, Sound.BLOCK_GLASS_BREAK, 1.1f, 0.8f);
                    broken += maps.damageSphere(point, 2, 14, Arenas.Damage.ACID);
                }
                case QUAKE -> {
                    world.spawnParticle(Particle.BLOCK, point, 52, 3, 0.4, 1.1, 0.03, Material.TUFF.createBlockData());
                    world.playSound(point, Sound.ENTITY_WARDEN_STEP, 1, 0.7f);
                    broken += maps.damageSphere(point, 4, 24, Arenas.Damage.BLAST);
                }
                default -> throw new IllegalStateException("Unexpected weather impact: " + disaster);
            }
            for (Player player : players) {
                if (!alive.contains(player.getUniqueId()) || player.getLocation().distanceSquared(point) > 16) continue;
                switch (disaster) {
                    case THUNDER -> player.damage(10 * supplies.lightningFactor(player));
                    case HAIL -> {
                        if (roofY(player.getLocation()) != 0) continue;
                        player.damage(5 * supplies.hailFactor(player));
                    }
                    case QUAKE -> {
                        double factor = supplies.quakeFactor(player);
                        player.setVelocity(new Vector(0, 0.4 * factor, 0.45 * factor));
                        player.damage(4 * factor);
                    }
                    default -> throw new IllegalStateException("Unexpected weather damage: " + disaster);
                }
            }
            return true;
        });
    }

    private Location surfaceOf(Player player) {
        Location point = player.getLocation();
        double top = world.getHighestBlockYAt(point.getBlockX(), point.getBlockZ()) + 0.5;
        return point.clone().set(point.getX(), Math.max(top, point.getY() - 0.4), point.getZ());
    }

    private void hail(Location target, int active) {
        BlockDisplay display = world.spawn(target.clone().add(-0.45, 14, -0.45), BlockDisplay.class, body -> {
            body.setBlock(Material.BLUE_ICE.createBlockData());
            body.setPersistent(false);
            body.setTeleportDuration(4);
        });
        weatherHits.add(new WeatherHit(target, active + 3, display));
    }

    private void clearWeatherHits() {
        for (WeatherHit hit : weatherHits) {
            if (hit.hailstone() != null) hit.hailstone().remove();
        }
        weatherHits.clear();
    }

    private void launchMeteor(Location target, int active) {
        BlockDisplay body = world.spawn(target.clone().add(0, 19, 0), BlockDisplay.class, display -> {
            display.setBlock(Material.MAGMA_BLOCK.createBlockData());
            display.setTeleportDuration(4);
            display.setGlowing(true);
            display.setPersistent(false);
        });
        meteors.add(new Meteor(target, active + 3, body));
    }

    private int roofY(Location point) {
        for (int y = point.getBlockY() + 2; y <= 115; y++) {
            if (world.getBlockAt(point.getBlockX(), y, point.getBlockZ()).getType().isSolid()) return y;
        }
        return 0;
    }

    private void roundInfo(Player player) {
        tell(player, disaster == null ? "Автостарт через " + countdown + " с. Карта: " + currentMap.name
            : "Сейчас: " + currentMap.name + " / " + disaster.name + ". Все игроки войдут в следующий раунд.");
    }

    private boolean paused(Player player) {
        return getConfig().getBoolean("participation." + player.getUniqueId() + ".paused");
    }

    private boolean isAfk(Player player) {
        UUID id = player.getUniqueId();
        int current = Bukkit.getCurrentTick();
        return away.contains(id) || current - lastActivity.getOrDefault(id, current) >= AFK_TICKS;
    }

    private void markActive(Player player) {
        UUID id = player.getUniqueId();
        lastActivity.put(id, Bukkit.getCurrentTick());
        if (away.remove(id)) tell(player, "AFK снят. Ты снова доступен для раундов.");
    }

    private void toggleAfk(Player player) {
        UUID id = player.getUniqueId();
        if (away.contains(id)) {
            markActive(player);
        } else {
            away.add(id);
            if (alive.contains(id)) sendToTower(player);
            tell(player, "AFK включён. Любое движение или /storm afk вернёт тебя.");
        }
    }

    private void setPaused(Player player, boolean value) {
        if (paused(player) == value) return;
        getConfig().set("participation." + player.getUniqueId() + ".paused", value);
        saveConfig();
        if (value && alive.contains(player.getUniqueId())) sendToTower(player);
        if (!alive.contains(player.getUniqueId()) && !builders.contains(player.getUniqueId())) {
            player.getInventory().setItem(5, participationButton(player));
        }
        tell(player, value ? "Ты пропускаешь раунды. Снова участвовать: /storm join."
            : "Участие включено со следующего раунда.");
    }

    private ItemStack participationButton(Player player) {
        boolean skip = paused(player);
        return button("pause", skip ? Material.GRAY_DYE : Material.LIME_DYE,
            skip ? "Раунды на паузе" : "Ты участвуешь в раундах", "ПКМ переключает участие.", "AFK: /storm afk.");
    }

    private void toggleBuild(Player player) {
        if (!player.hasPermission("stormtrials.build")) {
            tell(player, "Только для администратора.");
            return;
        }
        UUID id = player.getUniqueId();
        if (builders.remove(id)) {
            getConfig().set("builders." + id, null);
            sendToTower(player);
            tell(player, "Строительство выключено. Ты участвуешь в следующем раунде.");
        } else {
            sendToTower(player);
            builders.add(id);
            getConfig().set("builders." + id, true);
            player.getInventory().clear();
            player.setGameMode(GameMode.CREATIVE);
            tell(player, "Строительство включено. Арена защищена. /storm setlobby — сохранить точку входа.");
        }
        saveConfig();
    }

    private void setLobby(Player player) {
        Location location = player.getLocation();
        if (!player.hasPermission("stormtrials.build") || !builders.contains(player.getUniqueId())) {
            tell(player, "Сначала включи /storm build.");
        } else if (location.getWorld() != world || protectedZone(location) || !location.getBlock().isPassable()
            || !location.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            tell(player, "Встань на твёрдую площадку вне зоны арены.");
        } else {
            lobby = location.clone();
            world.setSpawnLocation(lobby);
            getConfig().set("lobby-location", lobby);
            saveConfig();
            tell(player, "Новая точка лобби сохранена.");
        }
    }

    private boolean protectedZone(Location location) {
        return location.getBlockY() <= 118 && Math.abs(location.getBlockX()) <= 38 && Math.abs(location.getBlockZ()) <= 38;
    }

    private boolean canBuild(Player player, Location location) {
        return builders.contains(player.getUniqueId()) && !protectedZone(location);
    }

    private void spectate(Player player) {
        UUID id = player.getUniqueId();
        if (alive.contains(id)) {
            tell(player, "Сначала выйди из раунда через /storm lobby.");
            return;
        }
        if (builders.contains(id)) {
            tell(player, "Выключи /storm build, чтобы наблюдать за раундом.");
            return;
        }
        player.closeInventory();
        if (teleport(player, Arenas.spectator(world))) {
            watching.add(id);
            player.setGameMode(GameMode.SPECTATOR);
            tell(player, "Летай над ареной. Вернуться в башню: /storm lobby.");
        }
    }

    private void enterGame(Player player) {
        if (!managed.add(player.getUniqueId())) return;
        markActive(player);
        String key = "original-players." + player.getUniqueId();
        if (player.getWorld() != world && !getConfig().contains(key)) {
            getConfig().set(key + ".inventory", Arrays.asList(player.getInventory().getContents()));
            getConfig().set(key + ".ender-chest", Arrays.asList(player.getEnderChest().getContents()));
            getConfig().set(key + ".location", player.getLocation());
            getConfig().set(key + ".game-mode", player.getGameMode().name());
            saveConfig();
        }
        if (player.hasPermission("stormtrials.build") && getConfig().getBoolean("builders." + player.getUniqueId())) {
            builders.add(player.getUniqueId());
            if (player.getWorld() != world) teleport(player, lobby);
            player.setGameMode(GameMode.CREATIVE);
            tell(player, "Режим строительства активен. /storm build — закончить.");
            return;
        }
        sendToTower(player);
        tell(player, "Штормовой архипелаг. Раунды начинаются автоматически. Книга открывает достижения.");
    }

    private void sendToTower(Player player) {
        UUID id = player.getUniqueId();
        alive.remove(id);
        watching.remove(id);
        supplies.leave(player);
        removeEffect(player);
        player.closeInventory();
        teleport(player, lobby);
        if (builders.contains(id)) return;
        equip(player);
        player.getInventory().setItem(0, button("round", Material.CLOCK, "Таймер раунда", "ПКМ показывает время старта и карту."));
        player.getInventory().setItem(1, button("maps", Material.MAP, "Карты", "ПКМ показывает ротацию арен."));
        player.getInventory().setItem(2, button("shop", Material.EMERALD, "Магазин снаряжения",
            "ПКМ: купи расходники и аксессуары за монеты."));
        player.getInventory().setItem(3, button("kit", Material.ECHO_SHARD, "Аксессуары",
            "ПКМ — выбрать один открытый аксессуар.", "Открываются крафтом в раунде."));
        player.getInventory().setItem(4, button("spectate", Material.COMPASS, "Наблюдать", "ПКМ включает полёт над ареной."));
        player.getInventory().setItem(5, participationButton(player));
        player.getInventory().setItem(6, button("medals", Material.BOOK, "Достижения", "ПКМ открывает достижения.", "Статистика: /storm stats."));
        player.getInventory().setItem(7, button("scope", Material.SPYGLASS, "Подзорная труба", "Удерживай ПКМ, чтобы рассмотреть арену."));
        player.getInventory().setItem(8, button("help", Material.PAPER, "Как играть", "ПКМ показывает правила."));
    }

    private void equip(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.getInventory().setHeldItemSlot(0);
        player.setHealth(maxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(5);
        player.setFireTicks(0);
        player.setFallDistance(0);
    }

    private void knockOut(Player player, String reason) {
        boolean wasAlive = alive.contains(player.getUniqueId());
        sendToTower(player);
        if (wasAlive) {
            increment(player, "coins", 2);
            saveConfig();
        }
        player.showTitle(Title.title(Component.text("Ты выбыл", NamedTextColor.RED),
            Component.text(wasAlive ? reason + " +2 монеты" : reason, NamedTextColor.WHITE)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7f, 0.8f);
    }

    private boolean teleport(Player player, Location destination) {
        UUID id = player.getUniqueId();
        internalTeleport.add(id);
        try {
            return player.teleport(destination);
        } finally {
            internalTeleport.remove(id);
        }
    }

    private void openShop(Player player) {
        if (alive.contains(player.getUniqueId()) || watching.contains(player.getUniqueId())
            || builders.contains(player.getUniqueId())) {
            tell(player, "Магазин находится в башне. Возврат: /storm lobby.");
            return;
        }
        Inventory menu = Bukkit.createInventory(null, 27, Component.text("Магазин усилений"));
        fillShop(player, menu);
        shops.put(player.getUniqueId(), menu);
        player.openInventory(menu);
    }

    private void fillShop(Player player, Inventory menu) {
        Perk current = reserved(player);
        menu.setItem(4, button("", Material.SUNFLOWER, "Монеты: " + coins(player),
            current == null ? "Усиление на следующий раунд не выбрано." : "На следующий раунд: " + current.title));
        for (Perk perk : Perk.values()) {
            menu.setItem(perk.slot, button("buy_" + perk.name(), perk.icon, perk.title,
                perk.description, "Цена: " + perk.cost + " монет",
                current == perk ? "Куплено на следующий раунд" : "Нажми, чтобы купить"));
        }
        menu.setItem(22, button("close", Material.BARRIER, "Закрыть"));
    }

    private void buy(Player player, Inventory menu, Perk perk) {
        if (reserved(player) != null) {
            tell(player, "У тебя уже есть усиление на следующий раунд.");
            return;
        }
        int balance = coins(player);
        if (balance < perk.cost) {
            tell(player, "Не хватает монет.");
            return;
        }
        getConfig().set(score(player, "coins"), balance - perk.cost);
        getConfig().set(score(player, "perk"), perk.name());
        saveConfig();
        fillShop(player, menu);
        tell(player, "Куплено: " + perk.title + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) enterGame(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        alive.remove(id);
        watching.remove(id);
        managed.remove(id);
        lastActivity.remove(id);
        away.remove(id);
        builders.remove(id);
        shops.remove(id);
        supplies.leave(player);
        removeEffect(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(lobby.clone());
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) sendToTower(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (internalTeleport.contains(event.getPlayer().getUniqueId())) return;
        Location destination = event.getTo();
        boolean adminCommand = event.getPlayer().isOp() && event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND;
        if (destination == null || destination.getWorld() != world || !adminCommand) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || !event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        markActive(player);
        Location to = event.getTo();
        if (to.getWorld() != world) return;
        if (watching.contains(player.getUniqueId())) {
            if (Math.abs(to.getX()) > 50 || Math.abs(to.getZ()) > 52 || to.getY() < 79 || to.getY() > 175) {
                teleport(player, Arenas.spectator(world));
            }
        } else if (!builders.contains(player.getUniqueId()) && water(to)) {
            knockOut(player, "Вода вокруг платформы смертельна.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnCommand(PlayerCommandPreprocessEvent event) {
        String name = event.getMessage().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (name.equals("/spawn") || name.equals("/essentials:spawn") || name.equals("/essentialsspawn:spawn")) {
            event.setCancelled(true);
            sendToTower(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer().getWorld() != world) return;
        Player player = event.getPlayer();
        if (event.getHand() == EquipmentSlot.HAND) markActive(player);
        Location interaction = event.getClickedBlock() == null ? player.getLocation() : event.getClickedBlock().getLocation();
        if (canBuild(player, interaction)
            || (action(event.getItem()).equals("scope") && !alive.contains(player.getUniqueId()))) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND
            || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        switch (action(event.getItem())) {
            case "round" -> roundInfo(player);
            case "maps" -> {
                for (Arenas.MapKind map : Arenas.MapKind.values()) tell(player, map.name);
            }
            case "shop" -> supplies.openShop(player);
            case "kit" -> supplies.open(player);
            case "pause" -> setPaused(player, !paused(player));
            case "spectate" -> spectate(player);
            case "stats" -> stats(player);
            case "medals" -> achievements.open(player);
            case "help" -> {
                tell(player, "Бедствия рушат карту. Вода вокруг платформы смертельна.");
                tell(player, "Выживший получает 7 монет, выбывший — 2. /storm lobby возвращает в башню.");
            }
            case "lobby" -> sendToTower(player);
            case "heal" -> {
                if (alive.contains(player.getUniqueId()) && roundPerks.remove(player.getUniqueId()) == Perk.MEDKIT) {
                    player.getInventory().setItem(4, null);
                    player.setHealth(Math.min(maxHealth(player), player.getHealth() + 10));
                    tell(player, "Аптечка использована.");
                }
            }
            default -> { }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || player.getWorld() != world || canBuild(player, player.getLocation())) return;
        markActive(player);
        event.setCancelled(true);
        Inventory menu = shops.get(player.getUniqueId());
        if (menu == null || !menu.equals(event.getClickedInventory())) return;
        String action = action(event.getCurrentItem());
        if (action.equals("close")) {
            Bukkit.getScheduler().runTask(this, () -> player.closeInventory());
        } else if (action.startsWith("buy_")) {
            buy(player, menu, Perk.valueOf(action.substring(4)));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        shops.remove(event.getPlayer().getUniqueId(), event.getInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && player.getWorld() == world
            && !canBuild(player, player.getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.getWorld() != world) return;
        if (!alive.contains(player.getUniqueId()) || event instanceof EntityDamageByEntityEvent) {
            event.setCancelled(true);
        } else {
            if (boost == Boost.PADDED && event.getCause() == EntityDamageEvent.DamageCause.FALL) event.setDamage(event.getDamage() * 0.45);
            if (penalty == Penalty.FRAGILE) event.setDamage(event.getDamage() * 1.18);
            if (event.getFinalDamage() >= player.getHealth()) {
                event.setCancelled(true);
                knockOut(player, "Стихия тебя настигла.");
            }
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity().getWorld() == world) event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getWorld() == world && !canBuild(event.getPlayer(), event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld() == world && !canBuild(event.getPlayer(), event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onFlow(BlockFromToEvent event) {
        if (event.getBlock().getWorld() == world
            && (protectedZone(event.getBlock().getLocation()) || protectedZone(event.getToBlock().getLocation()))
            && (event.getBlock().getY() > Arenas.SEA || event.getToBlock().getY() > Arenas.SEA)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDebris(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock && event.getEntity().getWorld() == world) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        if (event.getEntity().getWorld() == world) {
            event.blockList().removeIf(block -> protectedZone(block.getLocation()));
        }
    }

    @EventHandler
    public void onExplosion(BlockExplodeEvent event) {
        if (event.getBlock().getWorld() == world) {
            event.blockList().removeIf(block -> protectedZone(block.getLocation()));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getWorld() == world && !canBuild(event.getPlayer(), event.getPlayer().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && player.getWorld() == world && !canBuild(player, player.getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer().getWorld() == world && !canBuild(event.getPlayer(), event.getPlayer().getLocation())) event.setCancelled(true);
    }

    private ItemStack button(String action, Material material, String name, String... lines) {
        ItemStack icon = new ItemStack(material);
        icon.editMeta(meta -> {
            meta.displayName(Component.text(name, NamedTextColor.AQUA));
            meta.lore(Arrays.stream(lines).map(line -> (Component) Component.text(line, NamedTextColor.GRAY)).toList());
            meta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, action);
        });
        return icon;
    }

    private String action(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(buttonKey, PersistentDataType.STRING, "");
    }

    private boolean water(Location point) {
        return point.getY() < 53 || point.getBlock().getType() == Material.WATER;
    }

    private void effect(Player player, PotionEffectType type) {
        player.addPotionEffect(new PotionEffect(type, 600, 0));
    }

    private void removeEffect(Player player) {
        Perk perk = roundPerks.remove(player.getUniqueId());
        if (perk != null && perk != Perk.MEDKIT) player.removePotionEffect(potion(perk));
    }

    private PotionEffectType potion(Perk perk) {
        return switch (perk) {
            case SPEED -> PotionEffectType.SPEED;
            case SHIELD -> PotionEffectType.RESISTANCE;
            case JUMP -> PotionEffectType.JUMP_BOOST;
            case FEATHER -> PotionEffectType.SLOW_FALLING;
            case MEDKIT -> throw new IllegalArgumentException("The medkit is a held item");
        };
    }

    private double maxHealth(Player player) {
        var health = player.getAttribute(Attribute.MAX_HEALTH);
        return health == null ? 20 : health.getValue();
    }

    int coins(Player player) {
        return getConfig().getInt(score(player, "coins"), 10);
    }

    boolean spend(Player player, int price) {
        int amount = coins(player);
        if (amount < price) return false;
        getConfig().set(score(player, "coins"), amount - price);
        return true;
    }

    boolean mayShop(Player player) {
        UUID id = player.getUniqueId();
        return managed.contains(id) && !watching.contains(id) && !builders.contains(id) && !alive.contains(id);
    }

    private Perk reserved(Player player) {
        String selected = getConfig().getString(score(player, "perk"));
        return selected == null ? null : Perk.valueOf(selected);
    }

    private void increment(Player player, String stat, int change) {
        String key = score(player, stat);
        int current = stat.equals("coins") ? coins(player) : getConfig().getInt(key);
        getConfig().set(key, current + change);
    }

    private String score(Player player, String field) {
        return "scores." + player.getUniqueId() + "." + field;
    }

    private void stats(Player player) {
        tell(player, "Раунды: " + getConfig().getInt(score(player, "played"))
            + " | Выживания: " + getConfig().getInt(score(player, "wins")) + " | Монеты: " + coins(player));
    }

    private void tell(CommandSender player, String text) {
        player.sendMessage(Component.text("[StormTrials] ", NamedTextColor.AQUA)
            .append(Component.text(text, NamedTextColor.WHITE)));
    }

    private void bar(Player player, String text) {
        player.sendActionBar(Component.text(text, NamedTextColor.AQUA));
    }

    private ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }
}

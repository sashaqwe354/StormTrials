package stormtrials;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

final class Supplies implements Listener {
    enum Resource {
        CLOTH("Ткань", Material.WHITE_WOOL),
        METAL("Сплав", Material.IRON_NUGGET),
        CRYSTAL("Кристалл", Material.AMETHYST_SHARD),
        CIRCUIT("Схема", Material.REPEATER),
        HERB("Травы", Material.FERN),
        RESIN("Смола", Material.RESIN_CLUMP);

        final String title;
        final Material icon;

        Resource(String title, Material icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    enum Gear {
        BANDAGE("Походный бинт", "Восстанавливает 3 сердца.", Material.PAPER, Resource.CLOTH, 1, Resource.HERB, 1),
        TONIC("Регенератор", "Лечит постепенно 8 секунд.", Material.HONEY_BOTTLE, Resource.HERB, 2, Resource.RESIN, 1),
        SPRINT("Спринтер", "Скорость II на 12 секунд.", Material.SUGAR, Resource.HERB, 1, Resource.CRYSTAL, 1),
        SPRING("Пружинные ботинки", "Прыжок II на 12 секунд.", Material.LEATHER_BOOTS, Resource.METAL, 1, Resource.RESIN, 1),
        PARACHUTE("Складной парашют", "Замедляет падение на 16 секунд.", Material.WHITE_BANNER, Resource.CLOTH, 2, Resource.METAL, 1),
        ARMOR("Ударная пластина", "Сопротивление II на 8 секунд.", Material.IRON_CHESTPLATE, Resource.METAL, 2, Resource.RESIN, 1),
        BUFFER("Кристаллический буфер", "Даёт 4 временных сердца.", Material.AMETHYST_CLUSTER, Resource.CRYSTAL, 2, Resource.CIRCUIT, 1),
        ANCHOR("Ветровой якорь", "Ослабляет толчки воронки на 12 секунд.", Material.IRON_CHAIN, Resource.METAL, 2, Resource.CLOTH, 1),
        NEUTRALIZER("Нейтрализатор кислоты", "Снижает урон дождя на 12 секунд.", Material.GLASS_BOTTLE, Resource.RESIN, 2, Resource.HERB, 1),
        BLAST_PAD("Противоударный мат", "Снижает урон метеоритов на 12 секунд.", Material.TURTLE_SCUTE, Resource.RESIN, 2, Resource.CLOTH, 1),
        BOOSTER("Ракетный прыжок", "Подбрасывает вверх для выхода на уступ.", Material.FIREWORK_ROCKET, Resource.CRYSTAL, 1, Resource.CIRCUIT, 1),
        DASH("Импульсный привод", "Рывок вперёд, чтобы уйти от удара.", Material.WIND_CHARGE, Resource.METAL, 1, Resource.CIRCUIT, 1),
        ADRENALINE("Адреналин", "Скорость и сопротивление на 8 секунд.", Material.BLAZE_POWDER, Resource.HERB, 2, Resource.CIRCUIT, 1),
        FIELD_KIT("Полевой комплект", "Даёт 2 сердца сразу и регенерацию.", Material.GOLDEN_APPLE, Resource.HERB, 2, Resource.CLOTH, 2),
        RADAR("Радар снабжения", "Показывает координаты всех аирдропов.", Material.CLOCK, Resource.CIRCUIT, 1, Resource.CRYSTAL, 1),
        SMOKE("Дымовая завеса", "Метеоры не выбирают тебя целью 12 секунд.", Material.GRAY_DYE, Resource.RESIN, 1, Resource.CIRCUIT, 1),
        LODESTONE("Амулет устойчивости", "Аксессуар: ослабляет порывы ветра.", Material.LODESTONE, Resource.METAL, 3, Resource.CRYSTAL, 2, true),
        HOOD("Капюшон химика", "Аксессуар: снижает урон кислотного дождя.", Material.LEATHER_HELMET, Resource.CLOTH, 3, Resource.RESIN, 2, true),
        CHARM("Талисман против удара", "Аксессуар: снижает урон метеоров.", Material.ECHO_SHARD, Resource.CRYSTAL, 3, Resource.METAL, 2, true),
        SATCHEL("Сумка сборщика", "Аксессуар: даёт больше ресурсов из аирдропа.", Material.BUNDLE, Resource.CLOTH, 3, Resource.CIRCUIT, 2, true),
        ICE_COAT("Термоплащ", "Снижает урон града на 12 секунд.", Material.POWDER_SNOW_BUCKET, Resource.CLOTH, 2, Resource.HERB, 1),
        GROUNDING("Заземлитель", "Ослабляет удар молнии на 12 секунд.", Material.LIGHTNING_ROD, Resource.METAL, 2, Resource.CIRCUIT, 1),
        STABILIZER("Сейсмостабилизатор", "Снижает урон и толчок землетрясения на 12 секунд.", Material.COPPER_BULB, Resource.METAL, 2, Resource.RESIN, 1),
        SURGE("Настой высоты", "Ускорение и высокий прыжок на 10 секунд.", Material.GLOW_BERRIES, Resource.HERB, 2, Resource.CRYSTAL, 1),
        SKYWALKER("Воздушный шаг", "Подбрасывает вверх и замедляет падение.", Material.PHANTOM_MEMBRANE, Resource.CLOTH, 2, Resource.CRYSTAL, 1),
        ICE_RUNE("Снежный оберег", "Аксессуар: снижает урон града.", Material.BLUE_ICE, Resource.CRYSTAL, 3, Resource.CLOTH, 2, true),
        STORM_RING("Кольцо громоотвода", "Аксессуар: смягчает удары молнии.", Material.COPPER_INGOT, Resource.METAL, 3, Resource.CIRCUIT, 2, true),
        SHOCK_BOOTS("Сапоги с амортизатором", "Аксессуар: ослабляет землетрясения.", Material.DIAMOND_BOOTS, Resource.RESIN, 3, Resource.METAL, 2, true);

        final String title;
        final String hint;
        final Material icon;
        final Resource first;
        final int firstCost;
        final Resource second;
        final int secondCost;
        final boolean accessory;

        Gear(String title, String hint, Material icon, Resource first, int firstCost, Resource second, int secondCost) {
            this(title, hint, icon, first, firstCost, second, secondCost, false);
        }

        Gear(String title, String hint, Material icon, Resource first, int firstCost, Resource second, int secondCost, boolean accessory) {
            this.title = title;
            this.hint = hint;
            this.icon = icon;
            this.first = first;
            this.firstCost = firstCost;
            this.second = second;
            this.secondCost = secondCost;
            this.accessory = accessory;
        }
    }

    private static final long BUFF_MS = 12_000;
    private static final int MAX_DROPS = 2;
    private static final List<Integer> DEFAULT_DROP_SECONDS = List.of(12, 42);
    private static final int[][] LANDING_POINTS = {
        {-22, -19}, {21, 19}, {-17, 15}, {16, -18}, {1, 17}, {-3, -17}, {24, 2}, {-23, 1}
    };

    private static final class Kit {
        final Map<Resource, Integer> resources = new EnumMap<>(Resource.class);
        final Map<Gear, Integer> gear = new EnumMap<>(Gear.class);
        final Map<Gear, Long> abilities = new EnumMap<>(Gear.class);
        final Set<PotionEffectType> effects = new HashSet<>();
        Gear accessory;
    }

    private static final class Drop {
        final BlockDisplay crate;
        final BlockDisplay parachute;
        final TextDisplay label;
        final Interaction hitbox;
        Location position;
        double ground;
        boolean landed;

        Drop(BlockDisplay crate, BlockDisplay parachute, TextDisplay label, Interaction hitbox, Location position, double ground) {
            this.crate = crate;
            this.parachute = parachute;
            this.label = label;
            this.hitbox = hitbox;
            this.position = position;
            this.ground = ground;
        }

        void remove() {
            crate.remove();
            parachute.remove();
            label.remove();
            hitbox.remove();
        }
    }

    private final StormTrials plugin;
    private final Achievements achievements;
    private final World world;
    private final Predicate<Player> active;
    private final Set<Integer> dropSeconds;
    private final Map<UUID, Kit> kits = new HashMap<>();
    private final Map<UUID, Inventory> menus = new HashMap<>();
    private final Set<UUID> shopping = new HashSet<>();
    private final List<Drop> drops = new ArrayList<>();
    private int nextDrop;

    Supplies(StormTrials plugin, World world, Achievements achievements, Predicate<Player> active) {
        this.plugin = plugin;
        this.world = world;
        this.achievements = achievements;
        this.active = active;
        plugin.getConfig().addDefault("airdrop-seconds", DEFAULT_DROP_SECONDS);
        plugin.getConfig().options().copyDefaults(true);
        List<Integer> configured = plugin.getConfig().getIntegerList("airdrop-seconds");
        List<Integer> valid = configured.stream().filter(second -> second >= 5 && second <= 55)
            .distinct().sorted().limit(2).toList();
        if (valid.size() != configured.size()) {
            plugin.getLogger().warning("airdrop-seconds accepts up to two distinct seconds from 5 to 55. Using " + valid);
        }
        dropSeconds = Set.copyOf(valid);
        plugin.saveConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::advanceDrops, 5L, 5L);
    }

    void enter(Player player) {
        Kit kit = new Kit();
        kit.accessory = selected(player);
        for (Gear gear : Gear.values()) {
            int count = plugin.getConfig().getInt(purchaseKey(player) + "." + gear.name());
            if (!gear.accessory && count > 0) kit.gear.put(gear, count);
        }
        if (!kit.gear.isEmpty()) {
            plugin.getConfig().set(purchaseKey(player), null);
            tell(player, "Покупки доставлены в полевой верстак.");
        }
        kits.put(player.getUniqueId(), kit);
    }

    void leave(Player player) {
        Kit kit = kits.remove(player.getUniqueId());
        if (kit != null) kit.effects.forEach(player::removePotionEffect);
        menus.remove(player.getUniqueId());
        shopping.remove(player.getUniqueId());
    }

    void endRound() {
        for (Drop drop : drops) drop.remove();
        drops.clear();
        for (UUID id : List.copyOf(kits.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) leave(player);
        }
        kits.clear();
    }

    void tick(int elapsed) {
        if (dropSeconds.contains(elapsed) && drops.size() < MAX_DROPS) {
            int[] point = LANDING_POINTS[nextDrop++ % LANDING_POINTS.length];
            double ground = surface(point[0], point[1]);
            Location start = new Location(world, point[0] + 0.5, ground + 19, point[1] + 0.5);
            BlockDisplay crate = world.spawn(start, BlockDisplay.class, display -> {
                display.setBlock(Material.BARREL.createBlockData());
                display.setGlowing(true);
                display.setPersistent(false);
                display.setTeleportDuration(5);
            });
            BlockDisplay parachute = world.spawn(start.clone().add(0, 2, 0), BlockDisplay.class, display -> {
                display.setBlock(Material.ORANGE_WOOL.createBlockData());
                display.setPersistent(false);
                display.setTeleportDuration(5);
            });
            TextDisplay label = world.spawn(start.clone().add(0, 1.4, 0), TextDisplay.class, display -> {
                display.text(Component.text("АИРДРОП", NamedTextColor.GOLD));
                display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                display.setPersistent(false);
                display.setTeleportDuration(5);
            });
            Interaction hitbox = world.spawn(start, Interaction.class, entity -> {
                entity.setInteractionWidth(1.4f);
                entity.setInteractionHeight(1.6f);
                entity.setResponsive(true);
                entity.setPersistent(false);
            });
            drops.add(new Drop(crate, parachute, label, hitbox, start, ground));
            plugin.getLogger().info("Airdrop launched at round second " + elapsed);
            for (Player player : world.getPlayers()) {
                if (active.test(player)) tell(player, "Аирдроп снижается: " + coords(start) + ".");
            }
        }
    }

    private void advanceDrops() {
        for (Drop drop : drops) {
            Location position = drop.position;
            drop.ground = surface(position.getBlockX(), position.getBlockZ());
            double y = drop.landed ? drop.ground : Math.max(drop.ground, position.getY() - 1.85);
            if (drop.landed && Math.abs(position.getY() - y) < 0.05) continue;
            drop.position = new Location(world, position.getX(), y, position.getZ());
            drop.crate.teleport(drop.position.clone().add(-0.5, 0, -0.5));
            drop.label.teleport(drop.position.clone().add(0, 1.38, 0));
            drop.hitbox.teleport(drop.position);
            if (!drop.landed && y <= drop.ground + 0.01) {
                drop.landed = true;
                drop.parachute.remove();
                drop.label.text(Component.text("АИРДРОП • ПКМ", NamedTextColor.GOLD));
                world.playSound(drop.position, Sound.BLOCK_BARREL_OPEN, 1.1f, 0.7f);
            } else if (!drop.landed) {
                drop.parachute.teleport(drop.position.clone().add(-0.5, 2.3, -0.5));
                world.spawnParticle(Particle.CLOUD, drop.position.clone().add(0, 2.4, 0), 5, 0.45, 0.18, 0.45, 0.01);
            }
        }
    }

    private double surface(int x, int z) {
        int top = world.getHighestBlockYAt(x, z);
        return Math.max(Arenas.PLATFORM, top) + 1.05;
    }

    @EventHandler
    public void onClaim(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Drop match = drops.stream().filter(drop -> drop.hitbox.equals(event.getRightClicked())).findFirst().orElse(null);
        if (match == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Kit kit = kits.get(player.getUniqueId());
        if (!match.landed || kit == null || !active.test(player) || player.getWorld() != world
            || player.getLocation().distanceSquared(match.position) > 25) return;
        drops.remove(match);
        match.remove();
        for (Resource resource : Resource.values()) kit.resources.merge(resource, kit.accessory == Gear.SATCHEL ? 4 : 3, Integer::sum);
        Resource extra = Resource.values()[ThreadLocalRandom.current().nextInt(Resource.values().length)];
        kit.resources.merge(extra, 2, Integer::sum);
        tell(player, "Аирдроп собран. Все материалы +3, дополнительный бонус: " + extra.title + " +2."
            + (kit.accessory == Gear.SATCHEL ? " Сумка дала ещё по одному материалу." : ""));
        achievements.track(player, "drops");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
    }

    @EventHandler
    public void onClaimAt(PlayerInteractAtEntityEvent event) {
        onClaim(event);
    }

    void open(Player player) {
        openMenu(player, false);
    }

    void openShop(Player player) {
        if (!plugin.mayShop(player)) {
            tell(player, "Покупки доступны в лобби между твоими раундами.");
            return;
        }
        openMenu(player, true);
    }

    private void openMenu(Player player, boolean shop) {
        boolean inRound = active.test(player);
        Inventory menu = Bukkit.createInventory(null, 54, Component.text(shop ? "Магазин снаряжения"
            : (inRound ? "Крафты и набор раунда" : "Аксессуары StormTrials")));
        if (shop) shopping.add(player.getUniqueId());
        else shopping.remove(player.getUniqueId());
        show(player, menu);
        menus.put(player.getUniqueId(), menu);
        player.openInventory(menu);
    }

    private void show(Player player, Inventory menu) {
        Kit kit = kits.get(player.getUniqueId());
        Gear selected = selected(player);
        boolean shop = shopping.contains(player.getUniqueId());
        for (Gear gear : Gear.values()) {
            if (kit == null && !gear.accessory && !shop) continue;
            List<String> lore = new ArrayList<>();
            lore.add(gear.hint);
            lore.add(gear.first.title + " ×" + gear.firstCost + " • " + gear.second.title + " ×" + gear.secondCost);
            if (shop) {
                if (gear.accessory && unlocked(player, gear)) {
                    lore.add("Уже открыт. Надень через /storm accessories.");
                } else {
                    lore.add("ЛКМ — купить за " + price(gear) + " монет");
                    lore.add(gear.accessory ? "Открывается навсегда." : "Доставляется в верстак в начале раунда.");
                }
            } else if (gear.accessory) {
                lore.add(unlocked(player, gear) ? (selected == gear ? "Выбран на следующий раунд" : "Разблокирован")
                    : "Изготовь в раунде");
                lore.add(kit == null ? "ЛКМ — надеть или снять" : "ЛКМ — разблокировать навсегда");
            } else {
                lore.add("В наборе: " + kit.gear.getOrDefault(gear, 0));
                lore.add("ЛКМ — изготовить | ПКМ — применить");
            }
            ItemStack display = icon(gear.icon, gear.title, lore);
            display.editMeta(meta -> {
                var modelData = meta.getCustomModelDataComponent();
                modelData.setStrings(List.of("stormtrials:" + gear.name().toLowerCase(Locale.ROOT)));
                meta.setCustomModelDataComponent(modelData);
            });
            menu.setItem(gear.ordinal(), display);
        }
        if (shop) menu.setItem(49, icon(Material.SUNFLOWER, "Монеты: " + plugin.coins(player),
            List.of("Расходников на следующий раунд: " + pending(player) + " / 3",
                "Стартовые усиления: /storm perks")));
        if (kit != null) {
            for (Resource material : Resource.values()) {
                int number = kit.resources.getOrDefault(material, 0);
                menu.setItem(45 + material.ordinal(), icon(material.icon, material.title + " ×" + number,
                    List.of("Материал из аирдропа. Сбрасывается после раунда.")));
            }
        }
        menu.setItem(53, icon(Material.BARRIER, "Закрыть", List.of()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory menu = menus.get(player.getUniqueId());
        if (menu == null || !menu.equals(event.getView().getTopInventory())) return;
        event.setCancelled(true);
        if (!menu.equals(event.getClickedInventory())) return;
        int slot = event.getSlot();
        if (slot == 53) {
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            return;
        }
        if (slot < 0 || slot >= Gear.values().length || event.getCurrentItem() == null) return;
        Gear recipe = Gear.values()[slot];
        Kit kit = kits.get(player.getUniqueId());
        if (shopping.contains(player.getUniqueId())) {
            if (event.isLeftClick() && plugin.mayShop(player)) buy(player, recipe);
        } else if (kit == null) {
            if (recipe.accessory && event.isLeftClick() && unlocked(player, recipe)) {
                String value = selected(player) == recipe ? null : recipe.name();
                plugin.getConfig().set(key(player) + ".equipped", value);
                plugin.saveConfig();
                tell(player, value == null ? "Аксессуар снят." : "На следующий раунд выбран: " + recipe.title + ".");
            }
        } else if (event.isLeftClick()) {
            craft(player, kit, recipe);
        } else if (event.isRightClick() && !recipe.accessory) {
            use(player, kit, recipe);
        }
        show(player, menu);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (menus.remove(id, event.getInventory())) shopping.remove(id);
    }

    private void buy(Player player, Gear gear) {
        if (gear.accessory && unlocked(player, gear)) {
            tell(player, "Этот аксессуар уже открыт.");
            return;
        }
        if (!gear.accessory && pending(player) >= 3) {
            tell(player, "На следующий раунд уже куплено три расходника.");
            return;
        }
        if (!plugin.spend(player, price(gear))) {
            tell(player, "Не хватает монет: нужно " + price(gear) + ".");
            return;
        }
        if (gear.accessory) {
            plugin.getConfig().set(key(player) + ".owned." + gear.name(), true);
            achievements.check(player);
            tell(player, "Аксессуар куплен: " + gear.title + ". Экипировка — /storm accessories.");
        } else {
            String path = purchaseKey(player) + "." + gear.name();
            plugin.getConfig().set(path, plugin.getConfig().getInt(path) + 1);
            tell(player, "Куплено на следующий раунд: " + gear.title + ".");
        }
        plugin.saveConfig();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.75f, 1.4f);
    }

    private String purchaseKey(Player player) {
        return "purchases." + player.getUniqueId();
    }

    private int pending(Player player) {
        ConfigurationSection order = plugin.getConfig().getConfigurationSection(purchaseKey(player));
        if (order == null) return 0;
        return order.getKeys(false).stream().mapToInt(order::getInt).sum();
    }

    private int price(Gear gear) {
        return (gear.accessory ? 16 : 4) + 2 * (gear.firstCost + gear.secondCost);
    }

    private void craft(Player player, Kit kit, Gear recipe) {
        if (recipe.accessory && unlocked(player, recipe)) {
            tell(player, "Этот аксессуар уже открыт. Надень его в лобби.");
            return;
        }
        int first = kit.resources.getOrDefault(recipe.first, 0);
        int second = kit.resources.getOrDefault(recipe.second, 0);
        if (first < recipe.firstCost || second < recipe.secondCost) {
            tell(player, "Для крафта не хватает материалов.");
            return;
        }
        kit.resources.put(recipe.first, first - recipe.firstCost);
        kit.resources.put(recipe.second, second - recipe.secondCost);
        if (recipe.accessory) {
            plugin.getConfig().set(key(player) + ".owned." + recipe.name(), true);
            plugin.saveConfig();
            tell(player, "Аксессуар открыт: " + recipe.title + ". Его можно надеть в лобби.");
        } else {
            kit.gear.merge(recipe, 1, Integer::sum);
            tell(player, "Изготовлено: " + recipe.title + ". ПКМ по рецепту — применить.");
        }
        achievements.track(player, "crafts");
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 0.8f, 1.2f);
    }

    private void use(Player player, Kit kit, Gear item) {
        int count = kit.gear.getOrDefault(item, 0);
        if (count <= 0 || !active.test(player)) {
            tell(player, "Сначала изготовь предмет или купи его в лобби.");
            return;
        }
        if (item == Gear.RADAR && drops.isEmpty()) {
            tell(player, "Сейчас аирдропов нет.");
            return;
        }
        kit.gear.put(item, count - 1);
        switch (item) {
            case BANDAGE -> heal(player, 6);
            case TONIC -> effect(player, PotionEffectType.REGENERATION, 8, 0);
            case SPRINT -> effect(player, PotionEffectType.SPEED, 12, 1);
            case SPRING -> effect(player, PotionEffectType.JUMP_BOOST, 12, 1);
            case PARACHUTE -> effect(player, PotionEffectType.SLOW_FALLING, 16, 0);
            case ARMOR -> effect(player, PotionEffectType.RESISTANCE, 8, 1);
            case BUFFER -> effect(player, PotionEffectType.ABSORPTION, 20, 1);
            case ANCHOR, NEUTRALIZER, BLAST_PAD, SMOKE, ICE_COAT, GROUNDING, STABILIZER ->
                kit.abilities.put(item, System.currentTimeMillis() + BUFF_MS);
            case BOOSTER -> player.setVelocity(new Vector(0, 0.9, 0));
            case DASH -> {
                Vector direction = player.getLocation().getDirection().setY(0);
                if (direction.lengthSquared() > 0.0001) player.setVelocity(direction.normalize().multiply(1.15).setY(0.19));
            }
            case ADRENALINE -> {
                effect(player, PotionEffectType.SPEED, 8, 0);
                effect(player, PotionEffectType.RESISTANCE, 8, 0);
            }
            case FIELD_KIT -> {
                heal(player, 4);
                effect(player, PotionEffectType.REGENERATION, 6, 0);
            }
            case SURGE -> {
                effect(player, PotionEffectType.SPEED, 10, 0);
                effect(player, PotionEffectType.JUMP_BOOST, 10, 1);
            }
            case SKYWALKER -> {
                player.setVelocity(new Vector(0, 0.72, 0));
                effect(player, PotionEffectType.SLOW_FALLING, 12, 0);
            }
            case RADAR -> drops.forEach(drop -> tell(player, "Аирдроп: " + coords(drop.position)));
            default -> throw new IllegalArgumentException("Accessories are equipped in the lobby");
        }
        if (item == Gear.SMOKE) {
            world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, player.getLocation().add(0, 0.8, 0), 25, 1, 0.5, 1, 0.02);
        }
        if (item == Gear.BOOSTER || item == Gear.DASH || item == Gear.SKYWALKER) {
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        }
        tell(player, "Применено: " + item.title + ".");
    }

    double windFactor(Player player) {
        return modifier(player, Gear.ANCHOR, Gear.LODESTONE, 0.22, 0.65);
    }

    double acidFactor(Player player) {
        return modifier(player, Gear.NEUTRALIZER, Gear.HOOD, 0.28, 0.73);
    }

    double blastFactor(Player player) {
        return modifier(player, Gear.BLAST_PAD, Gear.CHARM, 0.32, 0.72);
    }

    double hailFactor(Player player) {
        return modifier(player, Gear.ICE_COAT, Gear.ICE_RUNE, 0.27, 0.72);
    }

    double lightningFactor(Player player) {
        return modifier(player, Gear.GROUNDING, Gear.STORM_RING, 0.26, 0.68);
    }

    double quakeFactor(Player player) {
        return modifier(player, Gear.STABILIZER, Gear.SHOCK_BOOTS, 0.25, 0.67);
    }

    boolean obscured(Player player) {
        Kit kit = kits.get(player.getUniqueId());
        return kit != null && ability(kit, Gear.SMOKE);
    }

    private double modifier(Player player, Gear ability, Gear accessory, double activeFactor, double equippedFactor) {
        Kit kit = kits.get(player.getUniqueId());
        if (kit == null) return 1;
        if (ability(kit, ability)) return activeFactor;
        return kit.accessory == accessory ? equippedFactor : 1;
    }

    private boolean ability(Kit kit, Gear gear) {
        return kit.abilities.getOrDefault(gear, 0L) > System.currentTimeMillis();
    }

    private Gear selected(Player player) {
        String value = plugin.getConfig().getString(key(player) + ".equipped");
        if (value == null) return null;
        for (Gear gear : Gear.values()) {
            if (gear.accessory && gear.name().equals(value) && unlocked(player, gear)) return gear;
        }
        return null;
    }

    private boolean unlocked(Player player, Gear gear) {
        return plugin.getConfig().getBoolean(key(player) + ".owned." + gear.name());
    }

    private String key(Player player) {
        return "accessories." + player.getUniqueId();
    }

    private void effect(Player player, PotionEffectType type, int seconds, int level) {
        timedEffect(player, type, seconds * 20, level);
    }

    void timedEffect(Player player, PotionEffectType type, int ticks, int level) {
        Kit kit = kits.get(player.getUniqueId());
        if (kit != null && player.addPotionEffect(new PotionEffect(type, ticks, level))) kit.effects.add(type);
    }

    private void heal(Player player, double amount) {
        var max = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(Math.min(max == null ? 20 : max.getValue(), player.getHealth() + amount));
    }

    private String coords(Location location) {
        return String.format(Locale.ROOT, "X %d · Z %d", location.getBlockX(), location.getBlockZ());
    }

    private void tell(Player player, String text) {
        player.sendMessage(Component.text("[Снабжение] ", NamedTextColor.GOLD).append(Component.text(text, NamedTextColor.WHITE)));
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, NamedTextColor.GOLD));
            meta.lore(lore.stream().map(line -> (Component) Component.text(line, NamedTextColor.GRAY)).toList());
        });
        return item;
    }
}

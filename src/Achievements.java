package stormtrials;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

final class Achievements {
    private enum Medal {
        FIRST("Первый шторм", "Сыграй первый раунд.", Material.COMPASS, "scores", "played", 1),
        SURVIVOR("Выживший", "Переживи первое бедствие.", Material.TOTEM_OF_UNDYING, "scores", "wins", 1),
        VETERAN("Закалённый штормом", "Выживи 5 раз.", Material.SHIELD, "scores", "wins", 5),
        LEGEND("Легенда архипелага", "Выживи 15 раз.", Material.NETHER_STAR, "scores", "wins", 15),
        COURIER("Снабженец", "Собери первый аирдроп.", Material.BARREL, "progress", "drops", 1),
        QUARTERMASTER("Квартирмейстер", "Собери 5 аирдропов.", Material.BUNDLE, "progress", "drops", 5),
        CRAFT("Полевой мастер", "Создай первый предмет.", Material.CRAFTING_TABLE, "progress", "crafts", 1),
        ENGINEER("Инженер экспедиции", "Создай 10 предметов.", Material.SMITHING_TABLE, "progress", "crafts", 10),
        COLLECTOR("Коллекционер", "Открой первый аксессуар.", Material.ECHO_SHARD, "progress", "accessories", 1),
        TRAVELER("Путешественник", "Посети 10 разных арен.", Material.MAP, "progress", "maps", 10),
        STORMWATCH("Вопреки стихиям", "Переживи 7 разных бедствий.", Material.HEART_OF_THE_SEA, "progress", "disasters", 7),
        COMPLETE("Атлас катастроф", "Переживи все 30 разных бедствий.", Material.ENCHANTED_BOOK, "progress", "disasters", 30);

        final String name;
        final String hint;
        final Material icon;
        final String source;
        final String stat;
        final int target;

        Medal(String name, String hint, Material icon, String source, String stat, int target) {
            this.name = name;
            this.hint = hint;
            this.icon = icon;
            this.source = source;
            this.stat = stat;
            this.target = target;
        }
    }

    private final StormTrials plugin;

    Achievements(StormTrials plugin) {
        this.plugin = plugin;
    }

    void play(Player player, String map) {
        unique(player, "visited", map, "maps");
        check(player);
    }

    void win(Player player, String disaster) {
        unique(player, "survived", disaster, "disasters");
        check(player);
    }

    void track(Player player, String stat) {
        String path = path(player, "progress", stat);
        plugin.getConfig().set(path, plugin.getConfig().getInt(path) + 1);
        check(player);
        plugin.saveConfig();
    }

    private void unique(Player player, String group, String id, String stat) {
        String path = path(player, "progress", group);
        plugin.getConfig().set(path + "." + id, true);
        ConfigurationSection recorded = plugin.getConfig().getConfigurationSection(path);
        if (recorded != null) plugin.getConfig().set(path(player, "progress", stat), recorded.getKeys(false).size());
    }

    void check(Player player) {
        boolean unlocked = false;
        for (Medal medal : Medal.values()) {
            String key = path(player, "medals", medal.name());
            if (plugin.getConfig().getBoolean(key) || progress(player, medal) < medal.target) continue;
            plugin.getConfig().set(key, true);
            unlocked = true;
            player.sendMessage(Component.text("Достижение открыто: " + medal.name, NamedTextColor.GOLD));
        }
        if (unlocked) player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.25f);
    }

    void open(Player player) {
        check(player);
        plugin.saveConfig();
        Inventory view = Bukkit.createInventory(null, 27, Component.text("Достижения архипелага"));
        for (Medal medal : Medal.values()) {
            boolean unlocked = plugin.getConfig().getBoolean(path(player, "medals", medal.name()));
            int value = Math.min(progress(player, medal), medal.target);
            ItemStack item = new ItemStack(unlocked ? medal.icon : Material.GRAY_DYE);
            item.editMeta(meta -> {
                meta.displayName(Component.text(medal.name, unlocked ? NamedTextColor.GOLD : NamedTextColor.GRAY));
                meta.lore(List.of(Component.text(medal.hint), Component.text(value + " / " + medal.target,
                    unlocked ? NamedTextColor.GREEN : NamedTextColor.YELLOW)));
            });
            view.setItem(medal.ordinal(), item);
        }
        player.openInventory(view);
    }

    private int progress(Player player, Medal medal) {
        if (medal == Medal.COLLECTOR) {
            ConfigurationSection owned = plugin.getConfig().getConfigurationSection(path(player, "accessories", "owned"));
            return owned == null ? 0 : owned.getKeys(false).size();
        }
        return plugin.getConfig().getInt(path(player, medal.source, medal.stat));
    }

    private String path(Player player, String group, String suffix) {
        return group + "." + player.getUniqueId() + "." + suffix;
    }
}

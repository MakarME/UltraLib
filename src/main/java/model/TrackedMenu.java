package model;

import config.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TrackedMenu extends Menu {
    protected MessageManager manager = MessageManager.getInstance();

    private static final Map<Object, Set<TrackedMenu>> openInstancesByKey = new ConcurrentHashMap<>();

    protected TrackedMenu() {
        super();
    }

    protected TrackedMenu(Menu parent) {
        super(parent);
    }

    protected TrackedMenu(Menu parent, boolean returnMakesNewInstance) {
        super(parent, returnMakesNewInstance);
    }

    protected abstract Object getTrackingKey();

    @Override
    protected void onPostDisplay(Player viewer) {
        Object key = getTrackingKey();
        openInstancesByKey.computeIfAbsent(key, k -> new HashSet<>()).add(this);
    }

    @Override
    protected void onMenuClose(Player player, Inventory inventory) {
        Menu currentMenu = Menu.getMenu(player);
        if (currentMenu != this) {
            Object key = getTrackingKey();
            Set<TrackedMenu> instances = openInstancesByKey.get(key);
            if (instances != null) {
                instances.remove(this);
                if (instances.isEmpty()) {
                    openInstancesByKey.remove(key);
                }
            }
        }
    }

    public static void refreshAll(Object key) {
        Common.runLater(() -> {
            Set<TrackedMenu> instances = openInstancesByKey.get(key);
            if (instances != null) {
                for (TrackedMenu menu : new HashSet<>(instances)) {
                    menu.restartMenu();
                }
            }
        });
    }

    protected void refreshAll() {
        refreshAll(getTrackingKey());
    }

    public static void updateSlotForAll(Object key, int slot, ItemStack item) {
        Common.runLater(() -> {
            Set<TrackedMenu> instances = openInstancesByKey.get(key);
            if (instances != null) {
                for (TrackedMenu menu : instances) {
                    menu.setItem(slot, item);
                }
            }
        });
    }
}

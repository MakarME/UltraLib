package model;

import config.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPagged;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TrackedMenuPagged<T> extends MenuPagged<T> {
    private static final Map<Object, Set<TrackedMenuPagged<?>>> openInstancesByKey = new ConcurrentHashMap<>();

    protected MessageManager manager = MessageManager.getInstance();

    protected TrackedMenuPagged(Iterable<T> items) {
        super(items);
    }

    protected TrackedMenuPagged(Menu parent, Iterable<T> items) {
        super(parent, items);
    }

    protected TrackedMenuPagged(Menu parent, List<Integer> itemPositions, Iterable<T> items) {
        super(parent, itemPositions, items);
    }

    protected TrackedMenuPagged(int pageSize, Iterable<T> items) {
        super(pageSize, items);
    }

    protected TrackedMenuPagged(int pageSize, Menu parent, Iterable<T> items) {
        super(pageSize, parent, items);
    }

    protected TrackedMenuPagged(List<Integer> itemPositions, Iterable<T> items) {
        super(itemPositions, items);
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
            Set<TrackedMenuPagged<?>> instances = openInstancesByKey.get(key);
            if (instances != null) {
                instances.remove(this);
                if (instances.isEmpty()) {
                    openInstancesByKey.remove(key);
                }
            }
        }
    }

    public static void refreshAll(Object key) {
        Set<TrackedMenuPagged<?>> instances = openInstancesByKey.get(key);
        if (instances != null) {
            for (TrackedMenuPagged<?> menu : new HashSet<>(instances)) {
                menu.restartMenu();
            }
        }
    }

    protected void refreshAll() {
        refreshAll(getTrackingKey());
    }
}

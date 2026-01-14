package model;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.menu.Menu;

@FunctionalInterface
public interface MenuClickAction<T> {
    void accept(Player player, Menu menu, ClickType click, T item);
}

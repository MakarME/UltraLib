package model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.plugin.SimplePlugin;
import model.TrackedMenuPagged;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GlobalTimedMenuPagged<T> extends TrackedMenuPagged<T> {

    // Глобальный список всех пагинированных меню с таймером [cite: 194, 195]
    private static final Set<GlobalTimedMenuPagged<?>> tickingMenus = ConcurrentHashMap.newKeySet();
    private static BukkitTask timerTask; // Одна задача на весь сервер [cite: 196]

    public GlobalTimedMenuPagged(Menu parent, List<Integer> slotsToFill, Iterable<T> items) {
        super(parent, slotsToFill, items);
    }

    public GlobalTimedMenuPagged(List<Integer> slotsToFill, Iterable<T> items) {
        super(slotsToFill, items);
    }

    /**
     * Выполняется АСИНХРОННО раз в секунду[cite: 199].
     * Здесь удобно проверять, не истекли ли данные.
     */
    protected abstract void onAsyncTick();

    @Override
    protected void onPostDisplay(Player viewer) {
        super.onPostDisplay(viewer); // Отрабатывает логика TrackedMenu [cite: 200]
        tickingMenus.add(this);      // Добавляем в список для таймера [cite: 201]
        startTimer();                // Запускаем таймер [cite: 202]
    }

    @Override
    protected void onMenuClose(Player player, Inventory inventory) {
        super.onMenuClose(player, inventory);

        Menu currentMenu = Menu.getMenu(player);
        // Удаляем только если игрок реально закрыл меню [cite: 205, 206]
        if (currentMenu != this) {
            tickingMenus.add(this); // ВАЖНО: здесь должно быть tickingMenus.remove(this), исправляем логику из исходника
            tickingMenus.remove(this);
            stopTimerIfEmpty();
        }
    }

    private static synchronized void startTimer() {
        if (timerTask == null || timerTask.isCancelled()) {
            timerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), () -> {
                if (tickingMenus.isEmpty()) return;

                // ЭТАП 1: Вычисляем данные в фоне [cite: 208]
                for (GlobalTimedMenuPagged<?> menu : tickingMenus) {
                    try {
                        menu.onAsyncTick();
                    } catch (Exception e) {
                        Common.error(e, "Error in async pagged menu tick");
                    }
                }

                // ЭТАП 2: Рисуем в основном потоке [cite: 210]
                Bukkit.getScheduler().runTask(SimplePlugin.getInstance(), () -> {
                    for (GlobalTimedMenuPagged<?> menu : tickingMenus) {
                        Player viewer = menu.getViewer();
                        if (viewer != null && Menu.getMenu(viewer) == menu) {
                            menu.refreshAll();
                        }
                    }
                });
            }, 0L, 20L); // 20 тиков = 1 секунда [cite: 214]
        }
    }

    private static synchronized void stopTimerIfEmpty() {
        if (tickingMenus.isEmpty() && timerTask != null) {
            timerTask.cancel();
            timerTask = null; // [cite: 215]
        }
    }
}

package model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GlobalTimedMenu extends TrackedMenu {

    // Глобальный список всех меню, которым нужен таймер.
    // Используем ConcurrentSet для безопасности в асинхроне.
    private static final Set<GlobalTimedMenu> tickingMenus = ConcurrentHashMap.newKeySet();

    // Одна задача на весь сервер
    private static BukkitTask timerTask;

    public GlobalTimedMenu() {
        super();
    }

    public GlobalTimedMenu(Menu parent) {
        super(parent);
    }

    /**
     * Выполняется АСИНХРОННО раз в секунду.
     */
    protected abstract void onAsyncTick();

    @Override
    protected void onPostDisplay(Player viewer) {
        // 1. Сначала логика TrackedMenu (добавление в мапу по ключу)
        super.onPostDisplay(viewer);

        // 2. Добавляем в список для таймера
        tickingMenus.add(this);

        // 3. Запускаем таймер, если он еще не работает
        startTimer();
    }

    @Override
    protected void onMenuClose(Player player, Inventory inventory) {
        // Логика TrackedMenu (там проверка уже есть внутри)
        super.onMenuClose(player, inventory);

        // --- КОПИРУЕМ ЛОГИКУ ИЗ TrackedMenu ---

        // Получаем меню, которое сейчас открыто у игрока
        Menu currentMenu = Menu.getMenu(player);

        // Если currentMenu == this, значит это просто restartMenu() (обновление),
        // и мы НЕ должны удалять себя из таймера.
        // Удаляем только если игрок реально закрыл меню или перешел в другое.
        if (currentMenu != this) {
            tickingMenus.remove(this);
            stopTimerIfEmpty();
        }
    }

    // --- ГЛОБАЛЬНЫЙ ТАЙМЕР ---

    private static synchronized void startTimer() {
        if (timerTask == null || timerTask.isCancelled()) {
            // Запускаем асинхронную задачу (20 тиков = 1 секунда)
            timerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(SimplePlugin.getInstance(), () -> {
                if (tickingMenus.isEmpty()) return;

                // ЭТАП 1: Вычисляем данные (тяжелая логика в фоне)
                for (GlobalTimedMenu menu : tickingMenus) {
                    try {
                        menu.onAsyncTick();
                    } catch (Exception e) {
                        Common.error(e, "Error in async menu tick");
                    }
                }

                // ЭТАП 2: Рисуем (в основном потоке)
                Bukkit.getScheduler().runTask(SimplePlugin.getInstance(), () -> {
                    for (GlobalTimedMenu menu : tickingMenus) {
                        Player viewer = menu.getViewer();

                        // Проверяем, что игрок все еще смотрит именно это меню
                        if (viewer != null && Menu.getMenu(viewer) == menu) {
                            // ВАЖНО: Для таймера используем redraw(), чтобы не сбивать курсор.
                            // Если тебе принципиально нужен restartMenu(), замени тут,
                            // но пользоваться меню будет невозможно.
                            menu.redrawButtons();
                        }
                    }
                });

            }, 0L, 20L);
        }
    }

    private static synchronized void stopTimerIfEmpty() {
        if (tickingMenus.isEmpty() && timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
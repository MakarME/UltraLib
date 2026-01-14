package manager;

import model.ItemRenderQueue;import model.TrackedMenu;import model.TrackedMenuPagged;import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class AsyncItemManager {

    private static final AsyncItemManager instance = new AsyncItemManager();

    // Кеш готовых предметов: "clan_head_123" -> ItemStack
    private final Map<String, ItemStack> itemCache = new ConcurrentHashMap<>();

    // Защита от дубликатов: "clan_head_123" -> Set("CLAN_LIST", "ADMIN_PANEL")
    // Если предмет уже грузится, мы просто запоминаем, какие еще меню надо обновить по завершении
    private final Map<String, Set<String>> pendingMenus = new ConcurrentHashMap<>();

    public static AsyncItemManager getInstance() {
        return instance;
    }

    /**
     * Главный метод получения предмета.
     *
     * @param itemKey   Уникальный ключ предмета (например, "leader_UUID").
     * @param generator Логика создания предмета (выполнится асинхронно).
     * @param menuTag   Ключ меню (TrackedMenuPagged), которое нужно обновить по готовности.
     * @return Готовый ItemStack или null, если он еще грузится.
     */
    public ItemStack getItemOrLoad(String itemKey, Supplier<ItemStack> generator, String menuTag) {
        // 1. Если есть в кеше — отдаем сразу
        if (itemCache.containsKey(itemKey)) {
            return itemCache.get(itemKey);
        }

        // 2. Регистрируем, что это меню ждет этот предмет
        pendingMenus.computeIfAbsent(itemKey, k -> ConcurrentHashMap.newKeySet()).add(menuTag);

        // 3. Если этот предмет уже кто-то заказал (он в очереди), выходим.
        // Мы уже добавили menuTag в список ожидания выше.
        // Проверяем размер > 1, значит мы только что добавили второй тег, а задача уже была.
        if (pendingMenus.get(itemKey).size() > 1) {
            return null;
        }

        // 4. Запускаем загрузку через твою очередь (ItemRenderQueue)
        ItemRenderQueue.getInstance().queue(
                generator,
                (loadedItem) -> {
                    // А. Сохраняем в кеш
                    itemCache.put(itemKey, loadedItem);

                    // Б. Уведомляем ВСЕ меню, которые ждали этот предмет
                    Set<String> tagsToRefresh = pendingMenus.remove(itemKey);
                    if (tagsToRefresh != null) {
                        for (String tag : tagsToRefresh) {
                            // Обновляем все открытые меню этого типа
                            TrackedMenuPagged.refreshAll(tag);
                            TrackedMenu.refreshAll(tag);
                        }
                    }
                }
        );

        return null; // Предмет еще не готов
    }

    // Очистка кеша (например при выходе игрока или рестарте плагина)
    public void invalidate(String itemKey) {
        itemCache.remove(itemKey);
    }

    public void invalidateAll() {
        itemCache.clear();
    }

    public void remove(String itemKey) {
        itemCache.remove(itemKey);
    }
}

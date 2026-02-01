package model;

import manager.AsyncItemManager;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.button.Button;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DynamicMenuSource<T> {

    private final List<Button> buttons = new CopyOnWriteArrayList<>();
    private List<T> cachedData = new ArrayList<>();

    private final Map<String, Map<String, Object>> itemMetadata = new ConcurrentHashMap<>();

    private final String menuTrackingKey;
    private final Function<T, String> keyExtractor;

    private final BiFunction<T, Map<String, Object>, ItemStack> iconProvider;
    private final MenuClickAction<T> clickHandler;

    private ItemStack customLoadingItem = null;

    public DynamicMenuSource(String menuTrackingKey,
                             Function<T, String> keyExtractor,
                             BiFunction<T, Map<String, Object>, ItemStack> iconProvider,
                             MenuClickAction<T> clickHandler) {
        this.menuTrackingKey = menuTrackingKey;
        this.keyExtractor = keyExtractor;
        this.iconProvider = iconProvider;
        this.clickHandler = clickHandler;
    }

    public DynamicMenuSource(String menuTrackingKey,
                             Function<T, String> keyExtractor,
                             Function<T, ItemStack> simpleIconProvider,
                             MenuClickAction<T> clickHandler) {
        this(menuTrackingKey, keyExtractor, (item, meta) -> simpleIconProvider.apply(item), clickHandler);
    }

    public void setMeta(T item, String key, Object value) {
        String itemKey = keyExtractor.apply(item);

        // 1. Обновляем "глобальное" хранилище (чтобы при полной перезагрузке данные сохранились)
        itemMetadata.computeIfAbsent(itemKey, k -> new ConcurrentHashMap<>()).put(key, value);

        // 2. Обновляем ЖИВУЮ кнопку
        updateExistingButton(item, key, value, true);
    }

    public void removeMeta(T item, String key) {
        String itemKey = keyExtractor.apply(item);
        Map<String, Object> meta = itemMetadata.get(itemKey);

        if (meta != null) {
            meta.remove(key);
            if (meta.isEmpty()) itemMetadata.remove(itemKey);

            // 2. Обновляем ЖИВУЮ кнопку (удаляем ключ)
            updateExistingButton(item, key, null, false);
        }
    }

    private void updateExistingButton(T item, String key, Object value, boolean isAdd) {
        // Ищем кнопку в списке (без пересоздания)
        for (Button button : buttons) {
            if (button instanceof GenericAsyncButton) {
                GenericAsyncButton<T> asyncBtn = (GenericAsyncButton<T>) button;

                // Проверяем, та ли это кнопка (сравниваем данные)
                T btnData = asyncBtn.getData();
                if (btnData != null && btnData.equals(item)) {

                    // А. Меняем метаданные внутри самой кнопки
                    if (isAdd) {
                        asyncBtn.getMetadata().put(key, value);
                    } else {
                        asyncBtn.getMetadata().remove(key);
                    }

                    // Б. Инвалидируем кэш картинки
                    // Важно: Ключ предмета остался тем же, но генератор теперь выдаст другой ItemStack (шерсть)
                    String uniqueKey = menuTrackingKey + "_item_" + keyExtractor.apply(item);
                    AsyncItemManager.getInstance().remove(uniqueKey);

                    // В. Обновляем меню (визуально)
                    refreshMenus();
                    return; // Нашли и обновили, выходим
                }
            }
        }
    }

    public boolean hasMeta(T item, String key) {
        String itemKey = keyExtractor.apply(item);
        Map<String, Object> meta = itemMetadata.get(itemKey);
        return meta != null && meta.containsKey(key);
    }


    public DynamicMenuSource<T> setLoadingItem(ItemStack item) {
        this.customLoadingItem = item;
        return this;
    }

    public List<Button> getButtons() { return buttons; }

    public void add(T item) {
        buttons.add(createButton(item));
        refreshMenus();
    }

    public void setContent(List<T> items) {
        buttons.clear();
        for (T item : items) {
            buttons.add(createButton(item));
        }
        this.cachedData = new ArrayList<>(items);
        refreshMenus();
    }

    public void update(List<T> newData) {
        if (this.cachedData.size() != newData.size()) {
            setContent(newData);
            return;
        }
        boolean hasChanges = false;
        for (int i = 0; i < newData.size(); i++) {
            T oldItem = this.cachedData.get(i);
            T newItem = newData.get(i);
            if (!Objects.equals(oldItem, newItem)) {
                String oldKey = menuTrackingKey + "_item_" + keyExtractor.apply(oldItem);
                AsyncItemManager.getInstance().remove(oldKey);

                buttons.set(i, createButton(newItem));
                hasChanges = true;
            }
        }
        if (hasChanges) {
            this.cachedData = new ArrayList<>(newData);
            refreshMenus();
        }
    }

    private void refreshItemCache(T item) {
        String uniqueKey = menuTrackingKey + "_item_" + keyExtractor.apply(item);

        AsyncItemManager.getInstance().remove(uniqueKey);

        refreshMenus();
    }

    public void refreshItem(java.util.function.Predicate<T> matcher) {
        for (T item : cachedData) {
            if (matcher.test(item)) {
                refreshItemCache(item);
            }
        }
    }

    public void remove(T item) {
        itemMetadata.remove(keyExtractor.apply(item));

        buttons.removeIf(b -> {
            if (b instanceof GenericAsyncButton) {
                GenericAsyncButton<T> btn = (GenericAsyncButton<T>) b;
                if (btn.getData() == null) return item == null;
                boolean match = btn.getData().equals(item);
                if (match) {
                    String key = keyExtractor.apply(btn.getData());
                    AsyncItemManager.getInstance().remove(key);
                }
                return match;
            }
            return false;
        });
        refreshMenus();
    }

    public void replace(T oldItem, T newItem) {
        for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            if (b instanceof GenericAsyncButton) {
                GenericAsyncButton<T> btn = (GenericAsyncButton<T>) b;
                // Сравниваем именно данные
                if (btn.getData() != null && btn.getData().equals(oldItem)) {
                    // Создаем новую кнопку и ставим её на ТОТ ЖЕ индекс
                    buttons.set(i, createButton(newItem));
                    refreshMenus(); // Обновляем меню игрокам
                    return;
                }
            }
        }
    }

    private Button createButton(T item) {
        String itemKeyStr = keyExtractor.apply(item);
        String itemUniqueKey = menuTrackingKey + "_item_" + itemKeyStr;

        Map<String, Object> meta = itemMetadata.getOrDefault(itemKeyStr, new HashMap<>());

        return new GenericAsyncButton<>(
                item,
                meta,
                itemUniqueKey,
                menuTrackingKey,
                iconProvider,
                clickHandler,
                customLoadingItem,
                -1
        );
    }

    private void refreshMenus() {
        Common.runLater(() -> {
            TrackedMenuPagged.refreshAll(menuTrackingKey);
        });
    }
}
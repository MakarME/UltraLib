package model;

import manager.AsyncItemManager;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.button.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class DynamicMenuSource<T> {

    private final List<Button> buttons = new CopyOnWriteArrayList<>();

    private List<T> cachedData = new ArrayList<>();

    private final String menuTrackingKey;
    private final Function<T, String> keyExtractor;
    private final Function<T, ItemStack> iconProvider;
    private final MenuClickAction<T> clickHandler;

    private ItemStack customLoadingItem = null;

    public DynamicMenuSource(String menuTrackingKey,
                             Function<T, String> keyExtractor,
                             Function<T, ItemStack> iconProvider,
                             MenuClickAction<T> clickHandler) {
        this.menuTrackingKey = menuTrackingKey;
        this.keyExtractor = keyExtractor;
        this.iconProvider = iconProvider;
        this.clickHandler = clickHandler;
    }

    public DynamicMenuSource<T> setLoadingItem(ItemStack item) {
        this.customLoadingItem = item;
        return this;
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public void add(T item) {
        buttons.add(createButton(item));
        refreshMenus();
    }

    public void remove(T item) {
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
                Button newButton = createButton(newItem);

                String oldKey = menuTrackingKey + "_item_" + keyExtractor.apply(oldItem);
                AsyncItemManager.getInstance().remove(oldKey);

                buttons.set(i, newButton);
                hasChanges = true;
            }
        }

        if (hasChanges) {
            this.cachedData = new ArrayList<>(newData);
            refreshMenus();
        }
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

    public void refreshItem(java.util.function.Predicate<T> matcher) {
        boolean found = false;
        for (T item : cachedData) {
            if (matcher.test(item)) {
                String key = menuTrackingKey + "_item_" + keyExtractor.apply(item);

                manager.AsyncItemManager.getInstance().remove(key);
                found = true;
            }
        }

        if (found) {
            refreshMenus();
        }
    }

    public void setContent(List<T> items) {
        buttons.clear();
        for (T item : items) {
            buttons.add(createButton(item));
        }
        this.cachedData = new ArrayList<>(items);
        refreshMenus();
    }

    private Button createButton(T item) {
        String itemUniqueKey = menuTrackingKey + "_item_" + keyExtractor.apply(item);

        return new GenericAsyncButton<>(
                item,
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
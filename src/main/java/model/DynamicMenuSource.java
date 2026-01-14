package model;

import manager.AsyncItemManager;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.menu.button.Button;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class DynamicMenuSource<T> {

    private final List<Button> buttons = new CopyOnWriteArrayList<>();

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

    public void update(T item) {
        remove(item);
        add(item);
    }

    public void setContent(List<T> items) {
        buttons.clear();
        for (T item : items) {
            buttons.add(createButton(item));
        }
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
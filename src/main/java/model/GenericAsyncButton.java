package model;

import manager.AsyncItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import java.util.function.Function;

public class GenericAsyncButton<T> extends Button {

    private final T data;
    private final String uniqueItemKey;
    private final String menuTrackingKey;

    private final Function<T, ItemStack> iconGenerator;
    private final MenuClickAction<T> clickAction;
    private final ItemStack loadingItem;

    // Новое поле
    private final int slot;

    public GenericAsyncButton(T data,
                              String uniqueItemKey,
                              String menuTrackingKey,
                              Function<T, ItemStack> iconGenerator,
                              MenuClickAction<T> clickAction,
                              ItemStack loadingItem,
                              int slot) { // <--- Добавили слот
        this.data = data;
        this.uniqueItemKey = uniqueItemKey;
        this.menuTrackingKey = menuTrackingKey;
        this.iconGenerator = iconGenerator;
        this.clickAction = clickAction;
        this.loadingItem = loadingItem != null ? loadingItem : createDefaultLoadingItem();
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    public T getData() {
        return data;
    }

    @Override
    public final ItemStack getItem() {
        ItemStack item = AsyncItemManager.getInstance().getItemOrLoad(
                uniqueItemKey,
                () -> iconGenerator.apply(data),
                menuTrackingKey
        );
        // Возвращаем либо готовый предмет, либо сохраненную заглушку
        return item != null ? item : loadingItem;
    }

    // Статический дефолтный метод, чтобы не дублировать код
    private static ItemStack createDefaultLoadingItem() {
        return ItemCreator.of(CompMaterial.CLOCK, "&7Загрузка...").make();
    }

    @Override
    public void onClickedInMenu(Player player, Menu menu, ClickType click) {
        if (clickAction != null) {
            clickAction.accept(player, menu, click, data);
        }
    }

    public static <T> Builder<T> from(T data) {
        return new Builder<>(data);
    }

    // ================== BUILDER ==================

    public static class Builder<T> {
        private final T data;
        private String itemKey;
        private String menuKey;
        private Function<T, ItemStack> generator;
        private MenuClickAction<T> action;
        private ItemStack loadingItem;
        private int slot = -1; // -1 = без слота

        public Builder(T data) { this.data = data; }

        public Builder<T> itemKey(String key) { this.itemKey = key; return this; }
        public Builder<T> menuKey(String key) { this.menuKey = key; return this; }
        public Builder<T> icon(Function<T, ItemStack> generator) { this.generator = generator; return this; }
        public Builder<T> onClick(MenuClickAction<T> action) { this.action = action; return this; }
        public Builder<T> loadingItem(ItemStack item) { this.loadingItem = item; return this; }
        public Builder<T> loadingItem(ItemCreator creator) { this.loadingItem = creator.make(); return this; }

        // Метод установки слота
        public Builder<T> slot(int slot) { this.slot = slot; return this; }

        public GenericAsyncButton<T> build() {
            return new GenericAsyncButton<>(data, itemKey, menuKey, generator, action, loadingItem, slot);
        }
    }
}
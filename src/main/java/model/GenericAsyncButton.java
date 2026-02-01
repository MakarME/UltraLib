package model;

import manager.AsyncItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class GenericAsyncButton<T> extends Button {

    private final T data;
    private final Map<String, Object> metadata;

    private final String uniqueItemKey;
    private final String menuTrackingKey;

    private final BiFunction<T, Map<String, Object>, ItemStack> iconGenerator;
    private final MenuClickAction<T> clickAction;
    private final ItemStack loadingItem;
    private final int slot;

    public GenericAsyncButton(T data,
                              Map<String, Object> metadata,
                              String uniqueItemKey,
                              String menuTrackingKey,
                              BiFunction<T, Map<String, Object>, ItemStack> iconGenerator,
                              MenuClickAction<T> clickAction,
                              ItemStack loadingItem,
                              int slot) {
        this.data = data;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.uniqueItemKey = uniqueItemKey;
        this.menuTrackingKey = menuTrackingKey;
        this.iconGenerator = iconGenerator;
        this.clickAction = clickAction;
        this.loadingItem = loadingItem != null ? loadingItem : createDefaultLoadingItem();
        this.slot = slot;
    }

    public int getSlot() { return slot; }
    public T getData() { return data; }

    public Map<String, Object> getMetadata() { return metadata; }

    public Object getMeta(String key) {
        return metadata.get(key);
    }

    public boolean hasMeta(String key) {
        return metadata.containsKey(key);
    }

    @Override
    public final ItemStack getItem() {
        ItemStack item = AsyncItemManager.getInstance().getItemOrLoad(
                uniqueItemKey,
                () -> iconGenerator.apply(data, metadata),
                menuTrackingKey
        );
        return item != null ? item : loadingItem;
    }

    private static ItemStack createDefaultLoadingItem() {
        return ItemCreator.of(CompMaterial.CLOCK, "&7Загрузка...").make();
    }

    @Override
    public void onClickedInMenu(Player player, Menu menu, ClickType click) {
        if (clickAction != null) {
            clickAction.accept(player, menu, click, data);
        }
    }

    // ================== BUILDER (Обновленный) ==================

    public static <T> Builder<T> from(T data) { return new Builder<>(data); }

    public static class Builder<T> {
        private final T data;
        private Map<String, Object> metadata = new HashMap<>();
        private String itemKey;
        private String menuKey;
        private BiFunction<T, Map<String, Object>, ItemStack> generator;
        private MenuClickAction<T> action;
        private ItemStack loadingItem;
        private int slot = -1;

        public Builder(T data) { this.data = data; }

        public Builder<T> meta(String key, Object value) { this.metadata.put(key, value); return this; }
        public Builder<T> metaMap(Map<String, Object> map) { if(map != null) this.metadata.putAll(map); return this; }
        public Builder<T> itemKey(String key) { this.itemKey = key; return this; }
        public Builder<T> menuKey(String key) { this.menuKey = key; return this; }

        public Builder<T> icon(java.util.function.Function<T, ItemStack> generator) {
            this.generator = (d, m) -> generator.apply(d); return this;
        }
        public Builder<T> icon(BiFunction<T, Map<String, Object>, ItemStack> generator) {
            this.generator = generator; return this;
        }

        public Builder<T> onClick(MenuClickAction<T> action) { this.action = action; return this; }
        public Builder<T> loadingItem(ItemStack item) { this.loadingItem = item; return this; }
        public Builder<T> slot(int slot) { this.slot = slot; return this; }

        public GenericAsyncButton<T> build() {
            return new GenericAsyncButton<>(data, metadata, itemKey, menuKey, generator, action, loadingItem, slot);
        }
    }
}
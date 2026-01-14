package util;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.UUID;

public class ToastUtil {

    /**
     * Тип рамки уведомления
     */
    public enum ToastFrame {
        TASK("task"),       // Обычная рамка
        GOAL("goal"),       // Скругленная рамка
        CHALLENGE("challenge"); // Ромбовидная рамка (со звуком челленджа)

        private final String key;

        ToastFrame(String key) {
            this.key = key;
        }
    }

    /**
     * Отправить тост игроку
     *
     * @param player  Игрок
     * @param icon    Материал иконки
     * @param title   Заголовок (поддерживает цвета)
     * @param frame   Тип рамки
     */
    public static void send(Player player, Material icon, String title, ToastFrame frame) {
        // Генерируем уникальный ключ, чтобы тосты не конфликтовали
        NamespacedKey key = new NamespacedKey(SimplePlugin.getInstance(), "toast_" + UUID.randomUUID());

        // Собираем JSON достижения вручную
        JsonObject json = new JsonObject();

        JsonObject display = new JsonObject();

        // Иконка
        JsonObject iconJson = new JsonObject();
        iconJson.addProperty("item", "minecraft:" + icon.name().toLowerCase());
        display.add("icon", iconJson);

        // Текст
        JsonObject titleJson = new JsonObject();
        titleJson.addProperty("text", Common.colorize(title));
        display.add("title", titleJson);

        // Описание (обязательно должно быть, даже если пустое, иначе краш)
        JsonObject descJson = new JsonObject();
        descJson.addProperty("text", "");
        display.add("description", descJson);

        // Настройки
        display.addProperty("frame", frame.key);
        display.addProperty("announce_to_chat", false);
        display.addProperty("show_toast", true);
        display.addProperty("hidden", true); // Скрываем из меню достижений

        json.add("display", display);

        // Критерий (триггер)
        JsonObject criteria = new JsonObject();
        JsonObject trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:impossible");
        criteria.add("impossible", trigger);
        json.add("criteria", criteria);

        // 1. Загружаем достижение в сервер
        // getUnsafe() нужен, так как Bukkit API не дает создавать ачивки на лету штатно
        try {
            Bukkit.getUnsafe().loadAdvancement(key, json.toString());
        } catch (Exception e) {
            // Игнорируем, если что-то пошло не так при загрузке
            return;
        }

        // 2. Выдаем игроку
        if (player.isOnline()) {
            player.getAdvancementProgress(Bukkit.getAdvancement(key)).awardCriteria("impossible");
        }

        // 3. Удаляем мусор через тик
        Common.runLater(2, () -> {
            if (player.isOnline()) {
                try {
                    // Сначала забираем прогресс (на всякий случай)
                    player.getAdvancementProgress(Bukkit.getAdvancement(key)).revokeCriteria("impossible");
                } catch (NullPointerException ignored) {}
            }
            // Удаляем ачивку из реестра сервера
            Bukkit.getUnsafe().removeAdvancement(key);
        });
    }
}
